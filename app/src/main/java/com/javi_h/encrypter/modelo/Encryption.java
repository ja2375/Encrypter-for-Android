package com.javi_h.encrypter.modelo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by ja2375 on 02/01/2018.
 */

public class Encryption {

    /**
     * Cifrar archivo.
     * @param Contraseña
     * @param Archivo original
     * @param Archivo cifrado
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     */
    public static void encrypt(String key, File input, File output) throws IOException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, NoSuchProviderException, InvalidAlgorithmParameterException {
        FileInputStream fis = new FileInputStream(input);
        FileOutputStream fos = new FileOutputStream(output);
        byte[] salt = getSalt();
        SecretKeyFactory secKeyFactory = SecretKeyFactory.getInstance("PBEwithMD5AND256BITAES-CBC-OPENSSL");
        KeySpec spec = new PBEKeySpec(key.toCharArray(), salt, 65535, 256);
        SecretKey pbeSecretKey = secKeyFactory.generateSecret(spec);
        SecretKey aesSecret = new SecretKeySpec(pbeSecretKey.getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecureRandom randomSecureRandom = SecureRandom.getInstance("SHA1PRNG");
        byte[] iv = new byte[cipher.getBlockSize()];
        randomSecureRandom.nextBytes(iv);
        IvParameterSpec ivParams = new IvParameterSpec(iv);
        // Write IV bytes
        fos.write(iv);
        fos.write(salt);
        cipher.init(Cipher.ENCRYPT_MODE, aesSecret, ivParams);
        CipherOutputStream cos = new CipherOutputStream(fos, cipher);

        int b;
        byte[] d = new byte[8];
        while ((b = fis.read(d)) != -1) {
            cos.write(d, 0, b);
        }
        cos.flush();
        cos.close();
        fis.close();
    }

    /**
     * Descifrar archivo
     * @param Contraseña
     * @param Archivo cifrado
     * @param Archivo descifrado
     * @throws Exception
     */
    public static void decrypt(String key, File input, File output) throws Exception {

        FileInputStream fis = new FileInputStream(input);

        FileOutputStream fos = new FileOutputStream(output);
        byte[] iv = new byte[16];
        byte[] salt = new byte[32];
        if (16 != fis.read(iv) || 32 != fis.read(salt)) {
            throw new Exception("IV or salt too short");
        }

        SecretKeyFactory secKeyFactory = SecretKeyFactory.getInstance("PBEwithMD5AND256BITAES-CBC-OPENSSL");
        KeySpec spec = new PBEKeySpec(key.toCharArray(), salt, 65535, 256);
        SecretKey pbeSecretKey = secKeyFactory.generateSecret(spec);
        SecretKey aesSecret = new SecretKeySpec(pbeSecretKey.getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesSecret, new IvParameterSpec(iv));
        CipherInputStream cis = new CipherInputStream(fis, cipher);

        int b;
        byte[] d = new byte[8];
        while ((b = cis.read(d)) != -1) {
            fos.write(d, 0, b);
        }
        fos.flush();
        fos.close();
        cis.close();
    }

    /**
     * Generar salt
     * @return
     * @throws NoSuchAlgorithmException
     */
    private static byte[] getSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[32];
        sr.nextBytes(salt);
        return salt;
    }

}
