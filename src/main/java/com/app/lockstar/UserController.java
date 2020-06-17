package com.app.lockstar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
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

    @PostMapping(path="/add")
    @ResponseBody
    public ResponseEntity addNewUser (@RequestParam String name, @RequestParam String password) {
        try {
            Integer newUserId = userService.signUp(name, password);
            return new ResponseEntity(Integer.toString(newUserId), HttpStatus.ACCEPTED);
        } catch (DataAccessException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(path="/key")
    @ResponseBody
    public ResponseEntity registerKey (@RequestParam("name") String username, @RequestParam("password") String password, @RequestParam("key") MultipartFile file) {
        User user;
        try {
            user = userService.signIn(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        String filePath;
        try {
            filePath = s3Service.upload(file);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        user.setPublicKey(file.getOriginalFilename());
        userService.update(user);

        return new ResponseEntity(filePath, HttpStatus.ACCEPTED);
    }
}
