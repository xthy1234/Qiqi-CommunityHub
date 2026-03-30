package com.gcs.utils;

import cn.hutool.crypto.digest.DigestUtil;

public class MD5Util {
    
	/**
	 * @param text明文
	 * @param key密钥
	 * @return 密文
	 */

	public static String md5(String text) {

		String md5str = DigestUtil.md5Hex(text);
		return md5str;
	}

}
