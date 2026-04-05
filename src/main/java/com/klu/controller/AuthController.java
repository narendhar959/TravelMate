package com.klu.controller;

import com.klu.entity.User;
import com.klu.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserService service;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        try {
            User savedUser = service.signup(user);
            Map<String, Object> res = new HashMap<>();
            res.put("user", savedUser);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String userid = request.get("userid");
        String password = request.get("password");

        User user = service.login(userid, password);

        if (user == null) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Invalid credentials");
            return ResponseEntity.status(401).body(err);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("user", user);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<?> getUser(@PathVariable int id) {
        User user = service.getUser(id);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(user);
    }
}
