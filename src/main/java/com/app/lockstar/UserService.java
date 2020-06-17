package com.app.lockstar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public Integer signUp (String username, String password) throws DataAccessException {
        User newUser = new User();
        newUser.setName(username);
        newUser.setPassword(password);
        userRepository.save(newUser);

        return newUser.getId();
    }

    public User signIn (String username, String password) throws Exception {
        Optional<User> sameNameUser = userRepository.findByName(username);

        if (sameNameUser.isEmpty()
                || !sameNameUser.get().isSamePassword(password)) {
            throw new Exception("Cannot sign in.");
        }

        return sameNameUser.get();
    }

    public void update (User user) throws DataAccessException {
        userRepository.save(user);
    }
}
