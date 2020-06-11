package com.app.lockstar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping(path="/file")
public class FileController {
    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private S3Service s3Service;

    @PostMapping
    @ResponseBody
    public ResponseEntity uploadFile (@RequestParam("file") MultipartFile file, @RequestParam("file_key") MultipartFile fileKey) {
        String uploadedFilePath, uploadedFileKeyPath;
        try {
            uploadedFilePath = s3Service.upload(file);
            uploadedFileKeyPath = s3Service.upload(fileKey);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        File newFile = new File();
        newFile.setName(uploadedFilePath);
        newFile.setKey(uploadedFileKeyPath);
        fileRepository.save(newFile);

        return new ResponseEntity(newFile.getId(), HttpStatus.ACCEPTED);
    }
}