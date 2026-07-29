-- Oracle 11g 测试存储过程
-- 用途：验证 glxt-api 存储过程 INOUT 参数配置和执行逻辑。

CREATE OR REPLACE PROCEDURE double_value (
  p_num IN OUT NUMBER
)
AS
BEGIN
  p_num := p_num * 2;
END;
/

-- SQL Developer / PL/SQL Developer 手工验证：
DECLARE
  v_num NUMBER := 5;
BEGIN
  double_value(v_num);
  DBMS_OUTPUT.PUT_LINE(v_num);
END;
/

