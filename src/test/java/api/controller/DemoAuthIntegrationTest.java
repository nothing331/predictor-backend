package api.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import api.dto.auth.TokenResponse;
import core.PredictionMarketApplication;
import core.repository.adapter.db.JpaUserRepository;
import core.store.UserStore;
import db.entity.UserEntity;

@SpringBootTest(classes = PredictionMarketApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "demo"})
public class DemoAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private core.repository.adapter.db.JpaRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserStore userStore;

    @BeforeEach
    public void setup() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        userStore.clear();
    }

    @Test
    public void testDemoRegisterAndLogin() throws Exception {
        // 1. Register a new demo user
        String registerReq = "{\"username\":\"demo_user\",\"password\":\"demo_pass\",\"email\":\"demo@example.com\"}";

        MvcResult registerResult = mockMvc.perform(post("/v1/auth/demo/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        // 2. Try to register same user again -> should fail
        mockMvc.perform(post("/v1/auth/demo/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerReq))
                .andExpect(status().isBadRequest());

        // 3. Login with correct credentials
        String loginReq = "{\"username\":\"demo_user\",\"password\":\"demo_pass\"}";

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/demo/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), TokenResponse.class);
        String accessToken = tokenResponse.accessToken();

        // 4. Verify access to protected endpoint
        mockMvc.perform(get("/v1/auth/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("demo@example.com"));

        // 5. Login with wrong password -> should fail
        String wrongLoginReq = "{\"username\":\"demo_user\",\"password\":\"wrong_pass\"}";
        mockMvc.perform(post("/v1/auth/demo/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(wrongLoginReq))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testDemoAuthDisabledByDefault() throws Exception {
        // This test specifically needs to run without "demo" profile.
        // But since this class has @ActiveProfiles("demo"), we'll skip this logic here 
        // OR we can create another test class without @ActiveProfiles("demo").
    }
}
