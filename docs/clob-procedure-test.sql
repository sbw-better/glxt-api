-- Oracle 11g：验证 CLOB IN / OUT / INOUT；使用客户端传入超过 32767 字符的文本。
CREATE OR REPLACE PROCEDURE test_clob_api (
  p_input IN CLOB,
  p_output OUT CLOB,
  p_inout IN OUT CLOB
) AS
BEGIN
  p_output := p_input;
  p_inout := p_input;
END;
/
