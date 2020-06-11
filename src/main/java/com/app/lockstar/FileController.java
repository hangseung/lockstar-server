package com.app.lockstar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping(path="/file")
public class FileController {
    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

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
        newFile.setName(file.getOriginalFilename());
        newFile.setKey(fileKey.getOriginalFilename());
        fileRepository.save(newFile);

        return new ResponseEntity(newFile.getId(), HttpStatus.ACCEPTED);
    }

    @GetMapping("/{fileId}")
    @ResponseBody
    public ResponseEntity downloadFile (@PathVariable Integer fileId, @RequestParam("username") String username, @RequestParam("password") String password) {
        Optional<User> sameNameUser = userRepository.findByName(username);

        if (sameNameUser.isEmpty()
        || !sameNameUser.get().isSamePassword(password)) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        User user = sameNameUser.get();
        for (File file : user.getFile()) {
            if (file.getId().equals(fileId)) {
                try {
                    Resource resource = s3Service.download(file.getName());
                    return new ResponseEntity(resource, HttpStatus.ACCEPTED);
                } catch (IOException e) {
                    e.printStackTrace();
                    return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }

        return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
    }
}