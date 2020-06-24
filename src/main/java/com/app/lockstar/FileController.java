package com.app.lockstar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path="/file")
public class FileController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private FileService fileService;

    @Value("${server_private_key_location}")
    private String serverPrivateKeyLocation;

    @PostMapping
    @ResponseBody
    public ResponseEntity uploadFile (@RequestParam("username") String username, @RequestParam String password, @RequestParam("file") MultipartFile file, @RequestParam("file_key") MultipartFile encryptedFileKey) {
        User user;
        try {
            user = userService.signIn(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        String newFileName, newFileKeyName;
        try {
            newFileName = s3Service.upload(file);

            Resource encryptedFileKeyResource = fileService.convertMultipartFileToResource(encryptedFileKey);
            Resource serverPrivateKeyResource = s3Service.download(serverPrivateKeyLocation);
            Key serverPrivateKey = fileService.convertResourceToPrivateKey(serverPrivateKeyResource);
            Resource fileKey = fileService.decryptResourceWithKey(encryptedFileKeyResource, serverPrivateKey);

            newFileKeyName = s3Service.upload(fileKey);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        File newFile = new File();
        newFile.setName(newFileName);
        newFile.setOriginalFileName(file.getOriginalFilename());
        newFile.setKey(encryptedFileKey.getOriginalFilename());
        newFile.setKey(newFileKeyName);
        newFile.setOwnerUserId(user.getId());
        fileRepository.save(newFile);
        user.addFile(newFile);
        userRepository.save(user);

        return new ResponseEntity(newFile.getId(), HttpStatus.ACCEPTED);
    }

    @PostMapping("/{fileId}")
    @ResponseBody
    public ResponseEntity replaceFile (@PathVariable Integer fileId, @RequestParam("username") String username, @RequestParam("password") String password, @RequestParam("file") MultipartFile file) {
        User user;
        try {
            user = userService.signIn(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        if (!user.hasFilePermission(fileId)) {
            return new ResponseEntity("No permission.", HttpStatus.NOT_ACCEPTABLE);
        }

        Optional<File> originalFile = this.fileRepository.findById(fileId);
        if (originalFile.isEmpty()) {
            return new ResponseEntity("File does not exist.", HttpStatus.BAD_REQUEST);
        }
        File editedFile = originalFile.get();

        String newFileName;
        try {
            newFileName = s3Service.upload(file);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        editedFile.setName(newFileName);
        editedFile.setOriginalFileName(file.getOriginalFilename());
        fileRepository.save(editedFile);

        return new ResponseEntity(HttpStatus.ACCEPTED);
    }

    @GetMapping("/{fileId}")
    @ResponseBody
    public ResponseEntity downloadFile (@PathVariable Integer fileId, @RequestParam("username") String username, @RequestParam("password") String password) throws NoSuchAlgorithmException {
        User user;
        try {
            user = userService.signIn(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        if (!user.hasFilePermission(fileId)) {
            return new ResponseEntity("No permission.", HttpStatus.NOT_ACCEPTABLE);
        }

        File fileEntity = this.fileRepository.findById(fileId).get();

        if (fileEntity.isExpired()) {
            return new ResponseEntity("Expired file.", HttpStatus.BAD_REQUEST);
        }

        try {
            Resource fileResource = s3Service.download(fileEntity.getName());

            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + fileEntity.getName() + "\"").body(fileResource);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/key/{fileId}")
    @ResponseBody
    public ResponseEntity downloadFileKey (@PathVariable Integer fileId, @RequestParam("username") String username, @RequestParam("password") String password) throws NoSuchAlgorithmException {
        User user;
        try {
            user = userService.signIn(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        if (!user.hasFilePermission(fileId)) {
            return new ResponseEntity("No permission.", HttpStatus.NOT_ACCEPTABLE);
        }

        File fileEntity = this.fileRepository.findById(fileId).get();

        if (fileEntity.isExpired()) {
            return new ResponseEntity("Expired file.", HttpStatus.BAD_REQUEST);
        }

        try {
            Resource fileKeyResource = s3Service.download(fileEntity.getKey());
            Resource userPublicKeyResource = s3Service.download(user.getPublicKey());

            Key userPublicKey = fileService.convertResourceToPublicKey(userPublicKeyResource);

            Resource encryptedFileKeyResource = fileService.encryptResourceWithKey(fileKeyResource, userPublicKey);

            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + fileEntity.getName() + "\"").body(encryptedFileKeyResource);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/allow/{fileId}")
    @ResponseBody
    public ResponseEntity allowUsers (@PathVariable Integer fileId, @RequestParam("username") String username, @RequestParam("password") String password, @RequestParam("usernames") String allowingUsernames) {
        User user;
        try {
            user = userService.signIn(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        Optional<File> foundFile = fileRepository.findById(fileId);
        if (foundFile.isEmpty()) {
            return new ResponseEntity("File does not exist", HttpStatus.BAD_REQUEST);
        }
        File file = foundFile.get();

        if (!file.getOwnerUserId().equals(user.getId())) {
            return new ResponseEntity("No permission", HttpStatus.NOT_ACCEPTABLE);
        }

        List<User> allowingUsers = userRepository.findByNameIn(Arrays.asList(allowingUsernames.split(",")));
        for (User allowingUser : allowingUsers) {
            allowingUser.addFile(file);
            userRepository.save(allowingUser);
        }

        return new ResponseEntity(HttpStatus.ACCEPTED);
    }
}