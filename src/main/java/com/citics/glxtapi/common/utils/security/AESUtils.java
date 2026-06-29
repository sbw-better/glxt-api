package com.citics.glxtapi.common.utils.security;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;

@Slf4j
public class AESUtils {

    static {
        try {
            Security.addProvider(new BouncyCastleProvider());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //填充类型，DES加密把前面的AES改成DES即可
    public static final String AES_TYPE_5 = "AES/CBC/PKCS5Padding";
    public static final String AES_TYPE_7 = "AES/CBC/PKCS7Padding";

    //填充类型，DES加密把前面的AES改成DES即可
    public static final String AES_KEY = "QNIWP4o2ly1tt^y";
    public static final String IV = "qsd3$dDFqsd3$dDF";

    public String encode(String password) {
        try {
            IvParameterSpec ivParameterSpec = new IvParameterSpec(IV.getBytes());
            //两个参数，第一个为私钥字节数组， 第二个为加密方式 AES或者DES
            SecretKeySpec secretKeySpec = new SecretKeySpec(AES_KEY.getBytes(), "AES");

            Cipher cipher = Cipher.getInstance(AES_TYPE_5);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            //PKCS5Padding比PKCS7Padding效率高，PKCS7Padding可支持IOS加解密
            //初始化，此方法可以采用三种方式，按加密算法要求来添加。
            //（1）无第三个参数
            //（2）第三个参数为SecureRandom random = new SecureRandom();中random对象，随机数。(AES不可采用这种方法)
            //（3）采用此代码中的IVParameterSpec(ECB不能使用偏移量)
            //加密时使用:ENCRYPT_MODE; 解密时使用:DECRYPT_MODE;

            byte[] encryptedData = cipher.doFinal(password.getBytes());
            return Base64.getUrlEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception:{}", e.getMessage());
            return "";
        }
    }

    public String decode(String encodePassword) {
        try {
            byte[] decodeByte = Base64.getUrlDecoder().decode(encodePassword);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(IV.getBytes());
            SecretKeySpec secretKeySpec = new SecretKeySpec(AES_KEY.getBytes(), "AES");

            Cipher cipher = Cipher.getInstance(AES_TYPE_5);
            //解密模式
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] bytes = cipher.doFinal(decodeByte);
            return new String(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception{}", e.getMessage());
            return "";
        }
    }

    /**
     * 加密前端密码字段
     * @param password 加密的数据
     * @return 解密的结果
     */
    public static String encryptFromUi(String password) {
        try {
            IvParameterSpec ivParameterSpec = new IvParameterSpec(IV.getBytes());
            //两个参数，第一个为私钥字节数组， 第二个为加密方式 AES或者DES
            SecretKeySpec secretKey = new SecretKeySpec(AES_KEY.getBytes(), "AES");

            Cipher cipher = Cipher.getInstance(AES_TYPE_7);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            byte[] encryptedData = cipher.doFinal(password.getBytes());
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception{}", e.getMessage());
            return "";
        }
    }

    /**
     * 解密方法 解密前端密码字段
     * @param encodePassword 要解密的数据
     * @return 解密的结果
     */
    public static String desEncryptFromUi(String encodePassword) {
        //固定私钥
        String aes_key = "QNIWP4o2ly1tt^y";
        String iv = "qsd3$dDFqsd3$dDF";
        try {
            byte[] decodeByte = Base64.getDecoder().decode(encodePassword);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
            SecretKeySpec secretKeySpec = new SecretKeySpec(aes_key.getBytes(), "AES");

            Cipher cipher = Cipher.getInstance(AES_TYPE_7);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] bytes = cipher.doFinal(decodeByte);
            String originalString = new String(bytes, StandardCharsets.UTF_8);
            return originalString;
        } catch (Exception e) {
            return encodePassword;
        }
    }
}
