package api.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.gson.GsonFactory;

import api.dto.auth.TokenResponse;
import core.user.User;
import core.service.UserService;
import core.repository.adapter.db.JpaUserRepository;
import db.entity.UserEntity;
import core.PredictionMarketApplication;

@SpringBootTest(classes = PredictionMarketApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private core.repository.adapter.db.JpaRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private core.service.UserService userService;

    @Autowired
    private core.store.UserStore userStore;

    @MockBean
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @BeforeEach
    public void setup() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        userStore.clear();
    }

    private GoogleIdToken createMockGoogleToken(String sub, String email, String name) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject(sub);
        payload.setEmail(email);
        payload.set("name", name);
        payload.setEmailVerified(true);
        payload.set("picture", "http://example.com/pic.jpg");

        // Use reflection or constructor to create a GoogleIdToken if possible. 
        // GoogleIdToken has a protected constructor, we need to create a subclass or use JsonWebSignature.Header
        // But the constructor is generic: GoogleIdToken(Header, Payload, byte[], byte[])
        com.google.api.client.json.webtoken.JsonWebSignature.Header header = new com.google.api.client.json.webtoken.JsonWebSignature.Header();
        header.setAlgorithm("RS256");

        try {
            return new GoogleIdToken(header, payload, new byte[0], new byte[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testFirstTimeUserCreation() throws Exception {
        // 1. POST /v1/auth/google with valid token -> creates user
        when(googleIdTokenVerifier.verify("valid_token"))
            .thenReturn(createMockGoogleToken("sub_123", "test@example.com", "Test User"));

        String reqBody = "{\"tokenId\":\"valid_token\"}";

        MvcResult result = mockMvc.perform(post("/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        // check DB for user
        assertEquals(1, userRepository.count());
        UserEntity user = userRepository.findAll().get(0);
        assertEquals("test@example.com", user.getEmail());
        assertEquals("sub_123", user.getGoogleSub());
        assertEquals(0, java.math.BigDecimal.valueOf(1000.0).compareTo(user.getBalance())); // default balance
    }

    @Test
    public void testSecondLoginDoesNotDuplicate() throws Exception {
        // 2. Second login with same googleSub doesn't duplicate
        when(googleIdTokenVerifier.verify("valid_token"))
            .thenReturn(createMockGoogleToken("sub_123", "test@example.com", "Test User"));

        String reqBody = "{\"tokenId\":\"valid_token\"}";

        // First login
        mockMvc.perform(post("/v1/auth/google").contentType(MediaType.APPLICATION_JSON).content(reqBody));
        // Second login
        mockMvc.perform(post("/v1/auth/google").contentType(MediaType.APPLICATION_JSON).content(reqBody));

        // DB should only have 1 user
        assertEquals(1, userRepository.count());
    }

    @Test
    public void testInvalidGoogleToken() throws Exception {
        // 3. Invalid token returns 401
        when(googleIdTokenVerifier.verify("invalid_token")).thenReturn(null);

        String reqBody = "{\"tokenId\":\"invalid_token\"}";

        mockMvc.perform(post("/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testAccessProtectedEndpoint() throws Exception {
        // 4. Access JWT grants access, 5. Missing/invalid JWT returns 401
        when(googleIdTokenVerifier.verify("valid_token"))
            .thenReturn(createMockGoogleToken("sub_123", "test@example.com", "Test User"));

        String reqBody = "{\"tokenId\":\"valid_token\"}";

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody))
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), TokenResponse.class);
        String accessToken = tokenResponse.accessToken();

        // Missing JWT -> 401
        mockMvc.perform(get("/v1/auth/me"))
                .andExpect(status().isUnauthorized());

        // Valid JWT -> 200
        mockMvc.perform(get("/v1/auth/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    public void testRefreshTokenRotation() throws Exception {
        // 6. Refresh token rotation, 7. Logout revokes
        when(googleIdTokenVerifier.verify("valid_token"))
            .thenReturn(createMockGoogleToken("sub_123", "test@example.com", "Test User"));

        // Login
        MvcResult loginResult = mockMvc.perform(post("/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tokenId\":\"valid_token\"}"))
                .andReturn();

        TokenResponse tokens = objectMapper.readValue(loginResult.getResponse().getContentAsString(), TokenResponse.class);

        // Refresh
        String refreshBody = "{\"refreshToken\":\"" + tokens.refreshToken() + "\"}";
        MvcResult refreshResult = mockMvc.perform(post("/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse newTokens = objectMapper.readValue(refreshResult.getResponse().getContentAsString(), TokenResponse.class);
        assertNotEquals(tokens.refreshToken(), newTokens.refreshToken());
        assertNotEquals(tokens.accessToken(), newTokens.accessToken());

        // Attempt refresh with old token -> Should fail
        mockMvc.perform(post("/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody))
                .andExpect(status().isUnauthorized());

        // Logout
        String logoutBody = "{\"refreshToken\":\"" + newTokens.refreshToken() + "\"}";
        mockMvc.perform(post("/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutBody))
                .andExpect(status().isOk());

        // Attempt refresh with revoked token -> fail
        mockMvc.perform(post("/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testPublicEndpointsWorkWithoutAuth() throws Exception {
        // 8. Public market endpoints still work
        mockMvc.perform(get("/v1/markets"))
                .andExpect(status().isOk());
    }

    @Test
    public void testSpoofedUserIdHeaderRejected() throws Exception {
        // 9. Spoofed userId rejected
        // Since auth changes have been implemented, passing a userId header alone won't bypass the JWT check on protected routes
        mockMvc.perform(get("/v1/auth/me")
                .header("userId", "fake_user_id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testExistingUserPersistencePreserved() throws Exception {
        // 10. Existing user state preserved
        when(googleIdTokenVerifier.verify("valid_token"))
            .thenReturn(createMockGoogleToken("sub_123", "test@example.com", "Test User"));

        // Login first time
        mockMvc.perform(post("/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tokenId\":\"valid_token\"}"))
                .andExpect(status().isOk());

        // Update user balance through UserService to ensure it's synced in both DB and UserStore
        core.user.User coreUser = userService.loadAll().stream()
                .filter(u -> "sub_123".equals(u.getGoogleSub()))
                .findFirst().get();
        coreUser.setBalance(java.math.BigDecimal.valueOf(500.0));
        userService.saveUser(coreUser);

        // Login second time (subsequent login)
        mockMvc.perform(post("/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tokenId\":\"valid_token\"}"))
                .andExpect(status().isOk());

        // Verify balance is still 500.0, not reset to 1000.0
        UserEntity updatedUser = userRepository.findAll().stream().filter(u -> "sub_123".equals(u.getGoogleSub())).findFirst().get();
        assertEquals(0, java.math.BigDecimal.valueOf(500.0).compareTo(updatedUser.getBalance()));
    }
}
