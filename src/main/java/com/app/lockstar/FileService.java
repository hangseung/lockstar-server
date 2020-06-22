package com.app.lockstar;

import com.amazonaws.util.IOUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import static java.util.Objects.requireNonNull;

@Service
public class FileService {
    public byte[] convertResourceToBytes (Resource resource) throws IOException {
        InputStream inputStream = resource.getInputStream();
        return IOUtils.toByteArray(inputStream);
    }

    public PublicKey convertResourceToPublicKey (Resource resource) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = convertResourceToBytes(resource);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public Resource encryptResourceWithPublicKey (Resource resource, PublicKey publicKey) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        byte[] resourceBytes = convertResourceToBytes(resource);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] iv = cipher.getIV();

        try (FileOutputStream fileOut = new FileOutputStream(requireNonNull(resource.getFilename()));
             CipherOutputStream cipherOut = new CipherOutputStream(fileOut, cipher)) {
            fileOut.write(iv);
            cipherOut.write(resourceBytes);
        }

        return new ByteArrayResource(resourceBytes);
    }
}
