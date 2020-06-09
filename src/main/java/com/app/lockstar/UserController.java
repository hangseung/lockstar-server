package com.app.lockstar;

import com.google.common.hash.Hashing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RestController
@RequestMapping(path="/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

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
}
