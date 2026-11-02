package com.example.demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.Entity.User;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String username, String password) {

        // 1. 检查用户名是否合法
        if (username == null || username.isBlank()) {
            throw new BusinessRuleException("Username cannot be blank");
        }

        // 2. 检查密码是否合法
        if (password == null || password.length() < 8) {
            throw new BusinessRuleException("Password must contain at least 8 characters");
        }

        // 3. 检查用户名是否重复
        if (userRepository.existsByUsername(username)) {
            throw new BusinessRuleException("Username already exists");
        }

        // 4. 创建用户
        User user = new User();

        // 5. 设置用户名
        user.setUsername(username);

        // 6. 生成并设置密码哈希
        user.setPasswordHash(passwordEncoder.encode(password));

        // 7. 保存并返回
        return userRepository.save(user);
    }

    

}