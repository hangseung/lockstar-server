package com.app.lockstar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
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
    private UserService userService;

    @Autowired
    private S3Service s3Service;

    @PostMapping
    @ResponseBody
    public ResponseEntity uploadFile (@RequestParam("username") String username, @RequestParam String password, @RequestParam("file") MultipartFile file, @RequestParam("file_key") MultipartFile fileKey) {
        User user;
        try {
            user = userService.signIn(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        try {
            s3Service.upload(file);
            s3Service.upload(fileKey);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        File newFile = new File();
        newFile.setName(file.getOriginalFilename());
        newFile.setKey(fileKey.getOriginalFilename());
        newFile.setOwnerUserId(user.getId());
        fileRepository.save(newFile);

        return new ResponseEntity(newFile.getId(), HttpStatus.ACCEPTED);
    }

    @GetMapping("/{fileId}")
    @ResponseBody
    public ResponseEntity downloadFile (@PathVariable Integer fileId, @RequestParam("username") String username, @RequestParam("password") String password) {
        User user;
        try {
            user = userService.signIn(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

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