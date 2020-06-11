package com.app.lockstar;

import com.google.common.hash.Hashing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RestController
@RequestMapping(path="/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3Service s3Service;

    @PostMapping(path="/add")
    @ResponseBody
    public ResponseEntity addNewUser (@RequestParam String name, @RequestParam String password) {
        Optional<User> sameNameUser = userRepository.findByName(name);
        if (sameNameUser.isPresent()) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        User newUser = new User();
        newUser.setName(name);
        newUser.setPassword(Hashing.sha256()
                .hashString(password, StandardCharsets.UTF_8)
                .toString());

        userRepository.save(newUser);

        return new ResponseEntity(HttpStatus.ACCEPTED);
    }

    @PostMapping(path="/key")
    @ResponseBody
    public ResponseEntity registerKey (@RequestParam("name") String name, @RequestParam("password") String password, @RequestParam("key") MultipartFile file) {
        Optional<User> user = userRepository.findByName(name);
        String hashedPassword = Hashing.sha256()
                .hashString(password, StandardCharsets.UTF_8)
                .toString();
        if (user.isEmpty()
        || !user.get().getPassword().equals(hashedPassword)) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        String filePath;
        try {
            filePath = s3Service.upload(file);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        user.get().setPublicKey(file.getOriginalFilename());
        userRepository.save(user.get());

        return new ResponseEntity(filePath, HttpStatus.ACCEPTED);
    }
}
