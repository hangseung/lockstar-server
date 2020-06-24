package com.app.lockstar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping(path="/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private S3Service s3Service;

    @Value("${server_public_key_location}")
    private String serverPublicKeyLocation;

    @PostMapping(path="/add")
    @ResponseBody
    public ResponseEntity addNewUser (@RequestParam("username") String username, @RequestParam("password") String password) {
        try {
            Integer newUserId = userService.signUp(username, password);
            return new ResponseEntity(Integer.toString(newUserId), HttpStatus.ACCEPTED);
        } catch (DataAccessException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(path="/key")
    @ResponseBody
    public ResponseEntity registerKey (@RequestParam("username") String username, @RequestParam("password") String password, @RequestParam("key") MultipartFile file) {
        User user;
        try {
            user = userService.signIn(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        String newPublicKeyName;
        try {
            newPublicKeyName = s3Service.upload(file);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        user.setPublicKey(newPublicKeyName);
        user.setOriginalPublicKeyName(file.getOriginalFilename());
        userService.update(user);

        return new ResponseEntity(HttpStatus.ACCEPTED);
    }

    @GetMapping(path="/server_public_key")
    @ResponseBody
    public ResponseEntity getServerPublicKey (@RequestParam("username") String username, @RequestParam("password") String password) {
        try {
            userService.signIn(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        try {
            Resource serverPublicKeyResource = s3Service.download(serverPublicKeyLocation);

            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + serverPublicKeyResource.getFilename() + "\"").body(serverPublicKeyResource);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
