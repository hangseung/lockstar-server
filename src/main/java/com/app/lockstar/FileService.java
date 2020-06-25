package com.app.lockstar;

import com.amazonaws.util.IOUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class FileService {
    public byte[] convertResourceToBytes (Resource resource) throws IOException {
        InputStream inputStream = resource.getInputStream();
        return IOUtils.toByteArray(inputStream);
    }

    public Key convertResourceToPublicKey (Resource resource) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = convertResourceToBytes(resource);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public Key convertResourceToPrivateKey (Resource resource) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = convertResourceToBytes(resource);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public Resource encryptResourceWithKey (Resource resource, Key key) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] resourceBytes = convertResourceToBytes(resource);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] bytePlain = cipher.doFinal(resourceBytes);
        byte[] byteEncrypted = Base64.getEncoder().encode(bytePlain);

        return new ByteArrayResource(byteEncrypted);
    }

    public Resource decryptResourceWithKey (Resource resource, Key key) throws NoSuchPaddingException, NoSuchAlgorithmException, IOException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] resourceBytes = convertResourceToBytes(resource);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] byteEncrypted = Base64.getDecoder().decode(resourceBytes);
        byte[] bytePlain = cipher.doFinal(byteEncrypted);

        return new ByteArrayResource(bytePlain);
    }

    public Resource convertMultipartFileToResource (MultipartFile multipartFile) throws IOException {
        byte[] bytes = multipartFile.getBytes();
        return new ByteArrayResource(bytes);
    }
}
