-- employees_test 存储过程接口配置导入测试脚本
-- 目标数据库：Oracle 11g
-- 前置条件：请先执行 docs/employees-procedure-test.sql 创建 employees_test 表。
-- 用途：为“接口配置-导入EXCEL”准备更贴近导入配置的测试过程和测试数据。

-- 1. 补充导入测试数据
-- 使用 NOT EXISTS 避免重复执行时报主键冲突。
INSERT INTO employees_test(emp_id, emp_name, salary, dept_id, create_time)
SELECT 101, '导入测试一', 4500, 40, SYSDATE - 7 FROM dual
 WHERE NOT EXISTS (SELECT 1 FROM employees_test WHERE emp_id = 101);

INSERT INTO employees_test(emp_id, emp_name, salary, dept_id, create_time)
SELECT 102, '导入测试二', 6500, 40, SYSDATE - 6 FROM dual
 WHERE NOT EXISTS (SELECT 1 FROM employees_test WHERE emp_id = 102);

INSERT INTO employees_test(emp_id, emp_name, salary, dept_id, create_time)
SELECT 103, '导入测试三', 8500, 40, SYSDATE - 5 FROM dual
 WHERE NOT EXISTS (SELECT 1 FROM employees_test WHERE emp_id = 103);

INSERT INTO employees_test(emp_id, emp_name, salary, dept_id, create_time)
SELECT 104, '导入测试四', 11000, 50, SYSDATE - 4 FROM dual
 WHERE NOT EXISTS (SELECT 1 FROM employees_test WHERE emp_id = 104);

INSERT INTO employees_test(emp_id, emp_name, salary, dept_id, create_time)
SELECT 105, '导入测试五', 7800, 50, SYSDATE - 3 FROM dual
 WHERE NOT EXISTS (SELECT 1 FROM employees_test WHERE emp_id = 105);

INSERT INTO employees_test(emp_id, emp_name, salary, dept_id, create_time)
SELECT 106, '导入测试六', 9800, 60, SYSDATE - 2 FROM dual
 WHERE NOT EXISTS (SELECT 1 FROM employees_test WHERE emp_id = 106);

COMMIT;

-- 2. IN + OUT + OUT CURSOR：按部门查询导入测试员工
CREATE OR REPLACE PROCEDURE proc_import_emp_by_dept (
  p_dept_id IN  NUMBER,
  p_status  OUT VARCHAR2,
  p_message OUT VARCHAR2,
  p_data    OUT SYS_REFCURSOR
)
AS
BEGIN
  OPEN p_data FOR
    SELECT emp_id, emp_name, salary, dept_id, create_time
      FROM employees_test
     WHERE dept_id = p_dept_id
     ORDER BY salary DESC, emp_id;

  p_status := '0';
  p_message := 'success';
END;
/

-- 3. IN + INOUT + OUT：调整单个员工薪资
-- p_adjust_amount 入参为调整金额，出参返回实际调整金额。
CREATE OR REPLACE PROCEDURE proc_import_salary_adjust (
  p_emp_id        IN     NUMBER,
  p_adjust_amount IN OUT NUMBER,
  p_new_salary    OUT    NUMBER,
  p_status        OUT    VARCHAR2,
  p_message       OUT    VARCHAR2
)
AS
  v_count NUMBER;
BEGIN
  SELECT COUNT(1)
    INTO v_count
    FROM employees_test
   WHERE emp_id = p_emp_id;

  IF v_count = 0 THEN
    p_adjust_amount := 0;
    p_new_salary := NULL;
    p_status := '404';
    p_message := 'employee not found';
    RETURN;
  END IF;

  UPDATE employees_test
     SET salary = salary + p_adjust_amount
   WHERE emp_id = p_emp_id;

  SELECT salary
    INTO p_new_salary
    FROM employees_test
   WHERE emp_id = p_emp_id;

  COMMIT;
  p_status := '0';
  p_message := 'salary adjust success';
EXCEPTION
  WHEN OTHERS THEN
    ROLLBACK;
    p_adjust_amount := 0;
    p_new_salary := NULL;
    p_status := TO_CHAR(SQLCODE);
    p_message := SUBSTR(SQLERRM, 1, 200);
END;
/

-- 4. IN + OUT + OUT CURSOR：按最低薪资汇总部门
CREATE OR REPLACE PROCEDURE proc_import_dept_summary (
  p_min_salary IN  NUMBER,
  p_status     OUT VARCHAR2,
  p_message    OUT VARCHAR2,
  p_data       OUT SYS_REFCURSOR
)
AS
BEGIN
  OPEN p_data FOR
    SELECT dept_id,
           COUNT(1) emp_count,
           MIN(salary) min_salary,
           MAX(salary) max_salary,
           SUM(salary) salary_total
      FROM employees_test
     WHERE salary >= NVL(p_min_salary, 0)
     GROUP BY dept_id
     ORDER BY dept_id;

  p_status := '0';
  p_message := 'success';
END;
/

-- 5. 数据库内快速验证
SET SERVEROUTPUT ON;

DECLARE
  v_status  VARCHAR2(20);
  v_message VARCHAR2(200);
  v_data    SYS_REFCURSOR;
BEGIN
  proc_import_emp_by_dept(40, v_status, v_message, v_data);
  DBMS_OUTPUT.PUT_LINE('proc_import_emp_by_dept: ' || v_status || ', ' || v_message);
  CLOSE v_data;
END;
/

DECLARE
  v_adjust    NUMBER := 500;
  v_new_salary NUMBER;
  v_status    VARCHAR2(20);
  v_message   VARCHAR2(200);
BEGIN
  proc_import_salary_adjust(101, v_adjust, v_new_salary, v_status, v_message);
  DBMS_OUTPUT.PUT_LINE('proc_import_salary_adjust: ' || v_status || ', adjust=' || v_adjust || ', salary=' || v_new_salary);
END;
/
