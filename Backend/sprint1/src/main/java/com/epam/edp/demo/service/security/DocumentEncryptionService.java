package com.epam.edp.demo.service.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

public class DocumentEncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_SIZE_BYTES = 12;
    private static final int KEY_SIZE_BYTES = 32;

    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    public DocumentEncryptionService(String keyBase64) {
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        if (keyBytes.length != KEY_SIZE_BYTES) {
            throw new IllegalStateException("Document encryption key must be 32 bytes (base64-encoded)");
        }
        this.key = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public byte[] encrypt(byte[] plainBytes) {
        byte[] iv = new byte[IV_SIZE_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plainBytes);
            return ByteBuffer.allocate(iv.length + cipherBytes.length)
                    .put(iv)
                    .put(cipherBytes)
                    .array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt uploaded document", e);
        }
    }

    public byte[] decrypt(byte[] encryptedBytes) {
        if (encryptedBytes.length <= IV_SIZE_BYTES) {
            throw new IllegalStateException("Corrupted encrypted payload");
        }
        ByteBuffer buffer = ByteBuffer.wrap(encryptedBytes);
        byte[] iv = new byte[IV_SIZE_BYTES];
        buffer.get(iv);
        byte[] cipherBytes = new byte[buffer.remaining()];
        buffer.get(cipherBytes);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(cipherBytes);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt stored document", e);
        }
    }
}
