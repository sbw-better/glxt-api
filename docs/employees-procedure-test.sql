-- employees_test 存储过程接口测试脚本
-- 目标数据库：Oracle 11g
-- 用途：覆盖 glxt-api 存储过程接口的 IN、OUT、INOUT、OUT CURSOR、可选条件、无数据、异常和多游标场景。

-- 1. 重建测试表
BEGIN
  EXECUTE IMMEDIATE 'DROP TABLE employees_test PURGE';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -942 THEN
      RAISE;
    END IF;
END;
/

CREATE TABLE employees_test (
  emp_id      NUMBER PRIMARY KEY,
  emp_name    VARCHAR2(50),
  salary      NUMBER,
  dept_id     NUMBER,
  create_time DATE DEFAULT SYSDATE
);

COMMENT ON TABLE employees_test IS 'glxt-api存储过程接口测试员工表';
COMMENT ON COLUMN employees_test.emp_id IS '员工ID';
COMMENT ON COLUMN employees_test.emp_name IS '员工姓名';
COMMENT ON COLUMN employees_test.salary IS '薪资';
COMMENT ON COLUMN employees_test.dept_id IS '部门ID';
COMMENT ON COLUMN employees_test.create_time IS '创建时间';

-- 2. 基础数据 + 补充边界数据
INSERT INTO employees_test(emp_id, emp_name, salary, dept_id, create_time)
VALUES (1, '张三', 5000, 10, SYSDATE - 10);

INSERT INTO employees_test(emp_id, emp_name, salary, dept_id, create_time)
VALUES (2, '李四', 7000, 10, SYSDATE - 8);

INSERT INTO employees_test(emp_id, emp_name, salary, dept_id, create_time)
VALUES (3, '王五', 9000, 20, SYSDATE - 6);

INSERT INTO employees_test(emp_id, emp_name, salary, dept_id, create_time)
VALUES (4, '赵六', 3000, 20, SYSDATE - 4);

INSERT INTO employees_test(emp_id, emp_name, salary, dept_id, create_time)
VALUES (5, '钱七', 12000, 30, SYSDATE - 2);

INSERT INTO employees_test(emp_id, emp_name, salary, dept_id, create_time)
VALUES (6, '孙八', 7000, 30, SYSDATE - 1);

COMMIT;

-- 3. IN + OUT 标量：按员工ID查询员工基础信息
CREATE OR REPLACE PROCEDURE proc_emp_get (
  p_emp_id   IN  NUMBER,
  p_emp_name OUT VARCHAR2,
  p_salary   OUT NUMBER,
  p_dept_id  OUT NUMBER,
  p_status   OUT VARCHAR2,
  p_message  OUT VARCHAR2
)
AS
BEGIN
  BEGIN
    SELECT emp_name, salary, dept_id
      INTO p_emp_name, p_salary, p_dept_id
      FROM employees_test
     WHERE emp_id = p_emp_id;

    p_status := '0';
    p_message := 'success';
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      p_emp_name := NULL;
      p_salary := NULL;
      p_dept_id := NULL;
      p_status := '404';
      p_message := 'employee not found';
  END;
END;
/

-- 4. INOUT：输入薪资，返回翻倍后的薪资
CREATE OR REPLACE PROCEDURE proc_salary_double (
  p_salary IN OUT NUMBER
)
AS
BEGIN
  p_salary := p_salary * 2;
END;
/

-- 5. IN + OUT CURSOR + OUT 标量：按部门和最低薪资查询员工列表
-- p_dept_id、p_min_salary 均允许传NULL，表示不启用该过滤条件。
CREATE OR REPLACE PROCEDURE proc_emp_query (
  p_dept_id    IN  NUMBER,
  p_min_salary IN  NUMBER,
  p_status     OUT VARCHAR2,
  p_message    OUT VARCHAR2,
  p_data       OUT SYS_REFCURSOR
)
AS
BEGIN
  OPEN p_data FOR
    SELECT emp_id, emp_name, salary, dept_id, create_time
      FROM employees_test
     WHERE (p_dept_id IS NULL OR dept_id = p_dept_id)
       AND (p_min_salary IS NULL OR salary >= p_min_salary)
     ORDER BY dept_id, salary, emp_id;

  p_status := '0';
  p_message := 'success';
END;
/

-- 6. IN + OUT：新增员工，覆盖DML过程和异常返回
CREATE OR REPLACE PROCEDURE proc_emp_insert (
  p_emp_id   IN  NUMBER,
  p_emp_name IN  VARCHAR2,
  p_salary   IN  NUMBER,
  p_dept_id  IN  NUMBER,
  p_status   OUT VARCHAR2,
  p_message  OUT VARCHAR2
)
AS
BEGIN
  INSERT INTO employees_test(emp_id, emp_name, salary, dept_id, create_time)
  VALUES (p_emp_id, p_emp_name, p_salary, p_dept_id, SYSDATE);

  COMMIT;
  p_status := '0';
  p_message := 'insert success';
EXCEPTION
  WHEN DUP_VAL_ON_INDEX THEN
    ROLLBACK;
    p_status := '409';
    p_message := 'employee id already exists';
  WHEN OTHERS THEN
    ROLLBACK;
    p_status := TO_CHAR(SQLCODE);
    p_message := SUBSTR(SQLERRM, 1, 200);
END;
/

-- 7. IN + OUT：按部门涨薪，覆盖更新类DML过程
CREATE OR REPLACE PROCEDURE proc_emp_raise_salary (
  p_dept_id    IN  NUMBER,
  p_raise_rate IN  NUMBER,
  p_rows       OUT NUMBER,
  p_status     OUT VARCHAR2,
  p_message    OUT VARCHAR2
)
AS
BEGIN
  UPDATE employees_test
     SET salary = salary * (1 + p_raise_rate)
   WHERE dept_id = p_dept_id;

  p_rows := SQL%ROWCOUNT;
  COMMIT;
  p_status := '0';
  p_message := 'raise salary success';
EXCEPTION
  WHEN OTHERS THEN
    ROLLBACK;
    p_rows := 0;
    p_status := TO_CHAR(SQLCODE);
    p_message := SUBSTR(SQLERRM, 1, 200);
END;
/

-- 8. OUT 双游标：用于验证多游标普通执行和Excel导出限制
CREATE OR REPLACE PROCEDURE proc_emp_multi_cursor (
  p_dept_data OUT SYS_REFCURSOR,
  p_top_data  OUT SYS_REFCURSOR
)
AS
BEGIN
  OPEN p_dept_data FOR
    SELECT dept_id, COUNT(1) emp_count, SUM(salary) salary_total
      FROM employees_test
     GROUP BY dept_id
     ORDER BY dept_id;

  OPEN p_top_data FOR
    SELECT emp_id, emp_name, salary, dept_id
      FROM employees_test
     WHERE salary >= 7000
     ORDER BY salary DESC, emp_id;
END;
/

-- 9. 数据库内快速验证
SET SERVEROUTPUT ON;

DECLARE
  v_name    VARCHAR2(50);
  v_salary  NUMBER;
  v_dept_id NUMBER;
  v_status  VARCHAR2(20);
  v_message VARCHAR2(200);
BEGIN
  proc_emp_get(1, v_name, v_salary, v_dept_id, v_status, v_message);
  DBMS_OUTPUT.PUT_LINE('proc_emp_get: ' || v_status || ', ' || v_message || ', ' || v_name || ', ' || v_salary);
END;
/

DECLARE
  v_salary NUMBER := 5000;
BEGIN
  proc_salary_double(v_salary);
  DBMS_OUTPUT.PUT_LINE('proc_salary_double: ' || v_salary);
END;
/

