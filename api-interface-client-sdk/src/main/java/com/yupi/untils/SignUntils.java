package com.yupi.untils;

import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;

public class SignUntils {

    public static String Sign(String body, String secretKey){
        String text = body + "." + secretKey;
        Digester md5 = new Digester(DigestAlgorithm.MD5);
        String content = md5.digestHex(text);
        return content;
    }
}
