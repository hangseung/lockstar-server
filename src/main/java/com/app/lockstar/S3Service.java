package com.app.lockstar;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

@Service
public class S3Service {

    private AmazonS3 s3Client;

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    @PostConstruct
    public void setS3Client() {
        AWSCredentials credentials = new BasicAWSCredentials(this.accessKey, this.secretKey);

        s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(this.region)
                .build();
    }

    private String getRandomString () {
        int leftLimit = 48, rightLimit = 122;
        int targetStringLength = 10;
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return generatedString;
    }

    public String upload(MultipartFile file) throws IOException {
        String fileName = this.getRandomString();

        File toPut = new File("./storage/" + fileName);
        toPut.createNewFile();
        FileOutputStream outputStream = new FileOutputStream(toPut);
        outputStream.write(file.getBytes());
        outputStream.close();

        PutObjectRequest request = new PutObjectRequest(bucket, fileName, toPut);
        s3Client.putObject(request);

        toPut.delete();

        return fileName;
    }

    public String upload (Resource resource) throws IOException {
        String fileName = this.getRandomString();
        InputStream inputStream = resource.getInputStream();
        PutObjectRequest request = new PutObjectRequest(bucket, fileName, inputStream, new ObjectMetadata());

        s3Client.putObject(request);

        return fileName;
    }

    public Resource download(String filename) throws IOException {
        S3Object object = s3Client.getObject(new GetObjectRequest(this.bucket, filename));
        S3ObjectInputStream objectInputStream = object.getObjectContent();
        byte[] bytes = IOUtils.toByteArray(objectInputStream);

        return new ByteArrayResource(bytes);
    }
}