package com.citics.glxtapi.plugin.db.domain;

import com.citics.glxtapi.common.utils.security.AESUtils;
import com.citics.glxtapi.common.utils.security.CryptoUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;

import static com.citics.glxtapi.plugin.db.domain.DataSourceProperty.ENC_PATTERN;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@Builder
@ApiModel(value = "DbConnectionDTO", description = "DB连接配置")
@Slf4j
public class DbConnectionInfo extends ConnectionBaseInfo {

    public DbConnectionInfo(ConnectionBaseInfo connectionBaseInfo) {
        this.setId(connectionBaseInfo.getId());
        this.setName(connectionBaseInfo.getName());
        this.setKey(connectionBaseInfo.getKey());
        this.setType(connectionBaseInfo.getType());
        this.setExtendConfigList(connectionBaseInfo.getExtendConfigList());
        this.setTimeout(connectionBaseInfo.getTimeout());
    }

    @ApiModelProperty(value = "账号")
    private String userName;

    @ApiModelProperty(value = "密码")
    private String password;

    @ApiModelProperty(value = "数据库连接URL")
    private String url;

    @ApiModelProperty(value = "驱动类")
    private String driverClassName;

    @ApiModelProperty(value = "数据源类型")
    private String dataSourceType;

    @ApiModelProperty(value = "最大行")
    private Integer maxRows;

    @JsonIgnore
    public String getDecryptPassword() {
        return decrypt(password);
    }

    /**
     * 字符串解密
     */
    private String decryptRsa(String cipherText) {
        if (StringUtils.hasText(cipherText)) {
            Matcher matcher = ENC_PATTERN.matcher(cipherText);
            if (matcher.find()) {
                try {
                    return CryptoUtils.decrypt(matcher.group(1));
                } catch (Exception e) {
                    log.error("DynamicDataSourceProperties.decrypt error ", e);
                }
            }
        }
        return cipherText;
    }

    /**
     * 字符串解密
     */
    private String decrypt(String cipherText) {
        if (StringUtils.hasText(cipherText)) {
            try {
                return AESUtils.desEncryptFromUi(cipherText);
            } catch (Exception e) {
                log.error("DynamicDataSourceProperties.decrypt error ", e);
            }
        }
        return cipherText;
    }
}
