package com.gcs.utils;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import cn.hutool.crypto.digest.DigestUtil;

public class EncryptUtil {
    
	/**
	 * md5算法
	 * @param text明文
	 * @param key密钥
	 * @return 密文
	 */

	public static String md5(String text) {
        if(text==null) return null;

		String md5str = DigestUtil.md5Hex(text);
		return md5str;
	}
	
	/**
	 * SHA-256算法
	 * @param text
	 * @return
	 * @throws Exception
	 */
    public static String sha256(String text) {
        if(text==null) return null;
    	StringBuilder stringBuilder = new StringBuilder();
        try {
			//获取SHA-256算法实例
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			//计算散列值
			byte[] digest = messageDigest.digest(text.getBytes());
			//将byte数组转换为15进制字符串
			for (byte b : digest) {
			    stringBuilder.append(Integer.toHexString((b & 0xFF) | 0x100), 1, 3);
			}
		} catch (NoSuchAlgorithmException e) {

			e.printStackTrace();
		}
        return stringBuilder.toString();
    }
    
    private static final String DES_ALGORITHM = "DES";

    /**
     * DES加密
     *
     * @param data 待加密的数据
     * @param key  密钥，长度必须为8位
     * @return 加密后的数据，使用Base64编码
     */
    public static String desEncrypt(String text) {
        if(text==null) return null;
        try {
            String key = "12345678";

			KeySpec keySpec = new DESKeySpec(key.getBytes());

			SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(DES_ALGORITHM);

			SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);


			Cipher cipher = Cipher.getInstance(DES_ALGORITHM);

			cipher.init(Cipher.ENCRYPT_MODE, secretKey);

			byte[] encryptedData = cipher.doFinal(text.getBytes());

			return Base64.getEncoder().encodeToString(encryptedData);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
        return null;
    }

    /**
     * DES解密
     *
     * @param encryptedData 加密后的数据，使用Base64编码
     * @param key 密钥，长度必须为8位
     * @return 解密后的数据
     */
    public static String desDecrypt(String text) {
        if(text==null) return null;
        try {
            String key = "12345678";

			KeySpec keySpec = new DESKeySpec(key.getBytes());

			SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(DES_ALGORITHM);

			SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);


			byte[] decodedData = Base64.getDecoder().decode(text);

			Cipher cipher = Cipher.getInstance(DES_ALGORITHM);

			cipher.init(Cipher.DECRYPT_MODE, secretKey);

			byte[] decryptedData = cipher.doFinal(decodedData);

			return new String(decryptedData);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
        return null;
    }
    
    private static final String AES_ALGORITHM = "AES";

    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private static final String AES_KEY = "1234567890123456";

    private static final String AES_IV = "abcdefghijklmnop";

    /**
     * AES加密
     *
     * @param data 待加密的数据
     * @return 加密后的数据，使用Base64编码
     */
    public static String aesEncrypt(String text) {
        if(text==null) return null;
        try {

			SecretKeySpec secretKeySpec = new SecretKeySpec(AES_KEY.getBytes(), AES_ALGORITHM);

			IvParameterSpec ivParameterSpec = new IvParameterSpec(AES_IV.getBytes());

			Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);

			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

			byte[] encryptedData = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

			return Base64.getEncoder().encodeToString(encryptedData);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
        return null;
    }

    /**
     * AES解密
     *
     * @param encryptedData 加密后的数据，使用Base64编码
     * @return 解密后的数据
     */
    public static String aesDecrypt(String text) {
        if(text==null) return null;
        try {

			SecretKeySpec secretKeySpec = new SecretKeySpec(AES_KEY.getBytes(), AES_ALGORITHM);

			IvParameterSpec ivParameterSpec = new IvParameterSpec(AES_IV.getBytes());

			Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);

			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

			byte[] decodedData = Base64.getDecoder().decode(text);

			byte[] decryptedData = cipher.doFinal(decodedData);

			return new String(decryptedData, StandardCharsets.UTF_8);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
        return null;
    }

}
