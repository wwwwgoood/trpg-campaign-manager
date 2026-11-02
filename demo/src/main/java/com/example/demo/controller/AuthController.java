package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.Entity.User;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.RegisterResponse;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final SecurityContextRepository securityContextRepository;

    private final CsrfTokenRepository csrfTokenRepository;

    /**
     * 构造器注入。
     */
    public AuthController(
            UserService userService,
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository
    ) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
    }


    /**
     * 注册用户。
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(
            @RequestBody RegisterRequest request
    ) {

        User user = userService.register(
            request.username(),
            request.password()
        );

        return new RegisterResponse(
            user.getId(),
            user.getUsername()
        );
    }

    /**
     * 获取 CSRF Token。
     */
    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }

    /**
     * 用户登录。
     */
    @PostMapping("/login")
    public RegisterResponse login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        System.out.println("LOGIN CONTROLLER REACHED");
        // 1. 认证用户名和密码
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.username(),
                    request.password()
                )
            );
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid username or password"
            );
        }

        // 2. 登录成功后更换原有 Session ID，防止 Session 固定攻击
        HttpSession session = httpRequest.getSession(false);

        if (session != null) {
            httpRequest.changeSessionId();
        }

        // 3. 清除登录前的 CSRF Token
        csrfTokenRepository.saveToken(
            null,
            httpRequest,
            httpResponse
        );

        // 4. 创建 SecurityContext
        SecurityContext context =
            SecurityContextHolder.createEmptyContext();

        // 5. 保存认证结果到 SecurityContext
        context.setAuthentication(authentication);

        // 6. 设置当前请求的认证上下文
        SecurityContextHolder.setContext(context);

        // 7. 持久化认证上下文，让后续请求能够恢复登录状态
        securityContextRepository.saveContext(
            context,
            httpRequest,
            httpResponse
        );

        // 8. 查询当前登录用户
        User user = userRepository
            .findByUsername(authentication.getName())
            .orElseThrow();

        // 9. 返回用户基本信息，不暴露密码哈希
        return new RegisterResponse(
            user.getId(),
            user.getUsername()
        );
    }
}