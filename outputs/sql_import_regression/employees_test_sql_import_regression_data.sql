-- 原SQL接口导入回归测试数据准备脚本（Oracle）
-- 用途：配合 api_sql_import_regression_test.xlsx 中的 SQL 查询接口执行验证。

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(1)
      INTO v_count
      FROM user_tables
     WHERE table_name = 'EMPLOYEES_TEST';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE employees_test (
                emp_id NUMBER PRIMARY KEY,
                emp_name VARCHAR2(50),
                salary NUMBER,
                dept_id NUMBER,
                create_time DATE DEFAULT SYSDATE
            )';
    END IF;
END;
/

MERGE INTO employees_test t
USING (
    SELECT 1 emp_id, '张三' emp_name, 5000 salary, 10 dept_id FROM dual
    UNION ALL SELECT 2, '李四', 7000, 10 FROM dual
    UNION ALL SELECT 3, '王五', 9000, 20 FROM dual
    UNION ALL SELECT 4, '赵六', 12000, 20 FROM dual
    UNION ALL SELECT 5, '钱七', 6500, 30 FROM dual
) s
ON (t.emp_id = s.emp_id)
WHEN MATCHED THEN
    UPDATE SET
        t.emp_name = s.emp_name,
        t.salary = s.salary,
        t.dept_id = s.dept_id
WHEN NOT MATCHED THEN
    INSERT (emp_id, emp_name, salary, dept_id)
    VALUES (s.emp_id, s.emp_name, s.salary, s.dept_id);

COMMIT;

SELECT emp_id, emp_name, salary, dept_id, create_time
  FROM employees_test
 ORDER BY emp_id;
