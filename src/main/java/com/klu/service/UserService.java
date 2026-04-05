package com.klu.service;

import com.klu.entity.User;
import com.klu.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired(required=false)
    private UserRepository repo;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public User signup(User user) {

        if (repo.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        if (repo.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // Sanitize gender: "prefer-not-to-say" → "prefer_not_to_say"
        if (user.getGender() != null) {
            String genderStr = user.getGender().name().replace("-", "_");
            user.setGender(User.Gender.valueOf(genderStr));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return repo.save(user);
    }

    public User login(String userid, String password) {
        Optional<User> userOpt = repo.findByEmail(userid);
        if (userOpt.isEmpty()) {
            userOpt = repo.findByUsername(userid);
        }

        if (userOpt.isPresent() &&
            passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return userOpt.get();
        }

        return null;
    }

    public User getUser(int id) {
        return repo.findById(id).orElse(null);
    }
}
