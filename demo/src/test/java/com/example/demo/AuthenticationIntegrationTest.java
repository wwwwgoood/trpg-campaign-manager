package com.example.demo;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.demo.Entity.User;
import com.example.demo.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void loginShouldPersistAuthenticationInSession() throws Exception {

        // 1. 准备模拟用户
        User alice = new User();
        alice.setUsername("alice");
        alice.setPasswordHash(
                passwordEncoder.encode("12345678"));

        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(alice));

        // 2. 模拟登录请求
        MvcResult result = mockMvc.perform(
                post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "alice",
                                    "password": "12345678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andReturn();

        // 3. 获取登录后的 Session
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        // 4. 使用同一个 Session 访问 /api/me
        mockMvc.perform(
                get("/api/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void unauthenticatedUserCannotAccessMe() throws Exception {

        mockMvc.perform(
                get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutShouldInvalidateSession() throws Exception {

        // 1. 准备模拟用户
        User alice = new User();
        alice.setUsername("alice");
        alice.setPasswordHash(
                passwordEncoder.encode("12345678"));

        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(alice));

        // 2. 登录
        MvcResult loginResult = mockMvc.perform(
                post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "alice",
                                    "password": "12345678"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        // 3. 获取登录后的 Session
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // 4. 注销
        mockMvc.perform(
                post("/api/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // 5. 验证旧 Session 已失效
        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
    }
}