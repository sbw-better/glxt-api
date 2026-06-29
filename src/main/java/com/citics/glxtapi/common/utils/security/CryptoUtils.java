package com.citics.glxtapi.common.utils.security;

import org.apache.tomcat.util.codec.binary.Base64;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CryptoUtils {

    public static final String DEFAULT_PUBLIC_KEY_STRING =
            "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJ4o6sn4WoPmbs7DR9m6QzuuUQM9erQTVPpwxIzBOETYkyKff0097qXVRL6AKPmaV+/siWewR7vpfYYjWajw5KkCAwEAAQ==";

    private static final String DEFAULT_PRIVATE_KEY_STRING =
            "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAniOqyfhag+ZuzsNH2Y6QzuUQM9erQTVPpwxIzBOETYkyKff0097qXVRL6AKPmaV+/siWewR7vpfYYjWajw5KkCAwEAAQ==" +
                    "kqQIDAQABAkBgFnbczRIewWFaE+GXTymUHUUV0lGmu7XdXUhzy6+CzkIcEnYpTgU1PGUyydiIyeiY8usvWKGjFWxLoKeJDY1wBAiEA5M9uqcx9XpL5uitLWHiiq7pR" +
                    "/Jm+nbB/wJqHVhZvLChKlQ9D/f7vHRWYMExHyHkCka7w5WyWUmNz00QKjZUIwIhAmqbo7JaF5GZzuj+qTsrZ7CYYtb2f414t7J6o6hV+BA1BXJuZ7r+fL" +
                    "6A+h9HUNQVcAtI2AhGNxT4aBgAOLNRQD/gIgC6qaZsQdnL9624SI1DwhBt4x24q3350pWwzgf14Kbbo=";

    public static String decrypt(String cipherText) throws Exception {
        return decrypt((String) null, cipherText);
    }

    public static String decrypt(String publicKeyText, String cipherText) throws Exception {
        PublicKey publicKey = getPublicKey(publicKeyText);
        return decrypt(publicKey, cipherText);
    }

    public static PublicKey getPublicKeyByX509(String x509File) {
        if (x509File == null || x509File.length() == 0) {
            return getPublicKey(null);
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(x509File);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate cer = (X509Certificate) factory.generateCertificate(in);
            return cer.getPublicKey();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to get public key", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static PublicKey getPublicKey(String publicKeyText) {
        if (publicKeyText == null || publicKeyText.length() == 0) {
            publicKeyText = DEFAULT_PUBLIC_KEY_STRING;
        }
        try {
            byte[] publicKeyBytes = Base64.decodeBase64(publicKeyText);
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "SunRsaSign");
            return keyFactory.generatePublic(x509KeySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to get public key", e);
        }
    }

    public static PublicKey getPublicKeyByPublicKeyFile(String publicKeyFile) {
        if (publicKeyFile == null || publicKeyFile.length() == 0) {
            return getPublicKey(null);
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(publicKeyFile);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int len = 0;
            byte[] b = new byte[512 / 8];
            while ((len = in.read(b)) != -1) {
                out.write(b, 0, len);
            }
            byte[] publicKeyBytes = out.toByteArray();
            X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory factory = KeyFactory.getInstance("RSA", "SunRsaSign");
            return factory.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to get public key", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String decrypt(PublicKey publicKey, String cipherText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        try {
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
        } catch (InvalidKeyException e) {
            // IBM JDK不支持私钥加密，公钥解密，反转公私钥
            RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
            RSAPrivateKeySpec spec = new RSAPrivateKeySpec(rsaPublicKey.getModulus(), rsaPublicKey.getPublicExponent());
            Key fakePrivateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, fakePrivateKey);
        }

        if (cipherText == null || cipherText.length() == 0) {
            return cipherText;
        }
        byte[] cipherBytes = Base64.decodeBase64(cipherText);
        byte[] plainBytes = cipher.doFinal(cipherBytes);
        return new String(plainBytes);
    }

    public static String encrypt(String plainText) throws Exception {
        return encrypt((String) null, plainText);
    }

    public static String encrypt(String key, String plainText) throws Exception {
        if (key == null) {
            key = DEFAULT_PRIVATE_KEY_STRING;
        }
        byte[] keyBytes = Base64.decodeBase64(key);
        return encrypt(keyBytes, plainText);
    }

    public static String encrypt(byte[] keyBytes, String plainText) throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA", "SunRsaSign");
        PrivateKey privateKey = factory.generatePrivate(spec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        try {
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        } catch (InvalidKeyException e) {
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) privateKey;
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(rsaPrivateKey.getModulus(), rsaPrivateKey.getPrivateExponent());
            Key fakePublicKey = KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, fakePublicKey);
        }
        return Base64.encodeBase64String(cipher.doFinal(plainText.getBytes("UTF-8")));
    }

    public static byte[][] genKeyPairBytes(int keySize) {
        byte[][] keyPairBytes = new byte[2][];
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA", "SunRsaSign");
            gen.initialize(keySize, new SecureRandom());
            KeyPair pair = gen.generateKeyPair();
            keyPairBytes[0] = pair.getPrivate().getEncoded();
            keyPairBytes[1] = pair.getPublic().getEncoded();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
        }
        return keyPairBytes;
    }

    public static String[] genKeyPair(int keySize) {
        byte[][] keyPairBytes = genKeyPairBytes(keySize);
        String[] keyPairs = new String[2];
        keyPairs[0] = Base64.encodeBase64String(keyPairBytes[0]);
        keyPairs[1] = Base64.encodeBase64String(keyPairBytes[1]);
        return keyPairs;
    }

    public static void main(String[] args) throws Exception {
        String username = encrypt("123456");
        System.out.println("加密userName:[" + username + "]");
        username = "ENC(" + username + ")";
        Pattern ENC_PATTERN = Pattern.compile("^ENC\\((.*)\\)$");
        Matcher matcher = ENC_PATTERN.matcher(username);
        if (matcher.find()) {
            System.out.println("解密userName:[" + decrypt(matcher.group(1)) + "]");
        }
    }
}
