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
    private core.repository.adapter.db.JpaTradeRepository tradeRepository;

    @Autowired
    private core.service.UserService userService;

    @Autowired
    private core.repository.port.MarketRepository marketRepository;

    @MockBean
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @MockBean
    private core.ratelimit.RateLimiterService rateLimiterService;

    @Autowired
    private core.service.JwtService jwtService;

    @BeforeEach
    public void setup() {
        refreshTokenRepository.deleteAll();
        tradeRepository.deleteAll();
        userRepository.deleteAll();
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
        assertEquals("USER", user.getRole()); // role is USER
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
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
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
                .andExpect(status().isNoContent());

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
    public void testAccountSummaryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/v1/users/me/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testMarketPositionRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/v1/markets/market-1/me"))
                .andExpect(status().isUnauthorized());
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
        assertEquals("USER", updatedUser.getRole());
    }

    @Test
    public void testRoleAuthorization() throws Exception {
        User admin = new User(java.util.UUID.randomUUID().toString());
        admin.setGoogleSub("admin_sub");
        admin.setEmail("admin@example.com");
        admin.setDisplayName("Admin User");
        admin.setRole(core.user.UserRole.ADMIN);
        userService.addUser(admin); // to create in DB

        String adminToken = jwtService.generateAccessToken(admin);

        User normalUser = new User(java.util.UUID.randomUUID().toString());
        normalUser.setGoogleSub("user_sub");
        normalUser.setEmail("user@example.com");
        normalUser.setDisplayName("Normal User");
        normalUser.setRole(core.user.UserRole.USER);
        userService.addUser(normalUser);

        String userToken = jwtService.generateAccessToken(normalUser);

        String createMarketReq = "{\"name\":\"Will it rain?\", \"description\":\"Will it rain today?\", \"closeTime\":\"2030-01-01T00:00:00Z\", \"category\":\"Weather\", \"yesLabel\":\"Yes\", \"noLabel\":\"No\"}";

        // Unauthenticated -> 401
        mockMvc.perform(post("/v1/markets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createMarketReq))
                .andExpect(status().isUnauthorized());

        // Authenticated USER -> 403
        mockMvc.perform(post("/v1/markets")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createMarketReq))
                .andExpect(status().isForbidden());

        // Authenticated ADMIN -> 200 (wait, it's 201 Created)
        mockMvc.perform(post("/v1/markets")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createMarketReq))
                .andExpect(status().isCreated());

        // Trading by USER -> works (no 403 or 401)
        mockMvc.perform(post("/v1/markets/test-market-id/trades")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertTrue(s != 401 && s != 403);
                });
    }

    @Test
    public void testManualAdminRoleSurvivesGoogleLogin() throws Exception {
        when(googleIdTokenVerifier.verify("valid_token"))
            .thenReturn(createMockGoogleToken("sub_123", "test@example.com", "Test User"));

        mockMvc.perform(post("/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tokenId\":\"valid_token\"}"))
                .andExpect(status().isOk());

        UserEntity promotedUser = userRepository.findAll().get(0);
        promotedUser.setRole("ADMIN");
        userRepository.save(promotedUser);

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tokenId\":\"valid_token\"}"))
                .andExpect(status().isOk())
                .andReturn();

        UserEntity updatedUser = userRepository.findAll().get(0);
        assertEquals("ADMIN", updatedUser.getRole());

        TokenResponse tokenResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                TokenResponse.class);

        mockMvc.perform(get("/v1/auth/me")
                .header("Authorization", "Bearer " + tokenResponse.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    public void testAccountSummaryReturnsEmptyRecentMarketsForUserWithNoTrades() throws Exception {
        User user = new User(java.util.UUID.randomUUID().toString());
        user.setEmail("empty-summary@example.com");
        user.setDisplayName("Empty Summary User");
        user.setBalance(new java.math.BigDecimal("321.45"));
        userService.addUser(user);

        String accessToken = jwtService.generateAccessToken(user);

        mockMvc.perform(get("/v1/users/me/summary")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.availableBalance").value(321.45))
                .andExpect(jsonPath("$.recentMarkets.length()").value(0));
    }

    @Test
    public void testAccountSummaryReturnsBalanceAndLastThreeMarkets() throws Exception {
        User user = new User(java.util.UUID.randomUUID().toString());
        user.setEmail("summary@example.com");
        user.setDisplayName("Summary User");
        user.setBalance(new java.math.BigDecimal("777.77"));
        userService.addUser(user);

        String accessToken = jwtService.generateAccessToken(user);

        core.market.Market marketA = new core.market.Market("summary-market-a", "Market A", "Desc");
        marketA.applyTrade(core.market.Outcome.YES, 12.0);
        core.market.Market marketB = new core.market.Market("summary-market-b", "Market B", "Desc");
        marketB.resolveMarket(core.market.Outcome.NO);
        core.market.Market marketC = new core.market.Market("summary-market-c", "Market C", "Desc");
        marketC.applyTrade(core.market.Outcome.NO, 8.0);
        core.market.Market marketD = new core.market.Market("summary-market-d", "Market D", "Desc");

        marketRepository.saveAll(java.util.List.of(marketA, marketB, marketC, marketD));

        tradeRepository.saveAll(java.util.List.of(
                new db.entity.TradeEntity(null, user.getUserId(), "summary-market-c", core.market.Outcome.YES,
                        new java.math.BigDecimal("3.5"), new java.math.BigDecimal("2.50"),
                        java.sql.Timestamp.from(java.time.Instant.parse("2026-03-27T12:00:00Z"))),
                new db.entity.TradeEntity(null, user.getUserId(), "summary-market-a", core.market.Outcome.YES,
                        new java.math.BigDecimal("2.0"), new java.math.BigDecimal("1.25"),
                        java.sql.Timestamp.from(java.time.Instant.parse("2026-03-27T11:00:00Z"))),
                new db.entity.TradeEntity(null, user.getUserId(), "summary-market-c", core.market.Outcome.NO,
                        new java.math.BigDecimal("1.0"), new java.math.BigDecimal("0.95"),
                        java.sql.Timestamp.from(java.time.Instant.parse("2026-03-27T10:30:00Z"))),
                new db.entity.TradeEntity(null, user.getUserId(), "summary-market-b", core.market.Outcome.NO,
                        new java.math.BigDecimal("4.0"), new java.math.BigDecimal("3.00"),
                        java.sql.Timestamp.from(java.time.Instant.parse("2026-03-27T10:00:00Z"))),
                new db.entity.TradeEntity(null, user.getUserId(), "summary-market-d", core.market.Outcome.YES,
                        new java.math.BigDecimal("5.0"), new java.math.BigDecimal("4.00"),
                        java.sql.Timestamp.from(java.time.Instant.parse("2026-03-27T09:00:00Z")))));

        mockMvc.perform(get("/v1/users/me/summary")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.availableBalance").value(777.77))
                .andExpect(jsonPath("$.recentMarkets.length()").value(3))
                .andExpect(jsonPath("$.recentMarkets[0].marketId").value("summary-market-c"))
                .andExpect(jsonPath("$.recentMarkets[0].userYesShares").value(3.5))
                .andExpect(jsonPath("$.recentMarkets[0].userNoShares").value(1.0))
                .andExpect(jsonPath("$.recentMarkets[0].projectedPayoutIfYes").value(3.5))
                .andExpect(jsonPath("$.recentMarkets[0].projectedPayoutIfNo").value(1.0))
                .andExpect(jsonPath("$.recentMarkets[1].marketId").value("summary-market-a"))
                .andExpect(jsonPath("$.recentMarkets[2].marketId").value("summary-market-b"))
                .andExpect(jsonPath("$.recentMarkets[2].currentYesChance").value(0.0))
                .andExpect(jsonPath("$.recentMarkets[2].currentNoChance").value(1.0));
    }

    @Test
    public void testMarketPositionReturnsEmptyStateForExistingMarketWithNoTrades() throws Exception {
        User user = new User(java.util.UUID.randomUUID().toString());
        user.setEmail("market-position-empty@example.com");
        user.setDisplayName("Market Position Empty");
        userService.addUser(user);

        String accessToken = jwtService.generateAccessToken(user);

        core.market.Market market = new core.market.Market("position-empty-market", "Position Empty Market", "Desc");
        marketRepository.saveAll(java.util.List.of(market));

        mockMvc.perform(get("/v1/markets/position-empty-market/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketId").value("position-empty-market"))
                .andExpect(jsonPath("$.tradeCount").value(0))
                .andExpect(jsonPath("$.yesSharesHeld").value(0.0))
                .andExpect(jsonPath("$.noSharesHeld").value(0.0))
                .andExpect(jsonPath("$.totalInvested").value(0.0))
                .andExpect(jsonPath("$.trades.length()").value(0));
    }

    @Test
    public void testMarketPositionReturnsResolvedPnlAndTradeHistory() throws Exception {
        User user = new User(java.util.UUID.randomUUID().toString());
        user.setEmail("market-position@example.com");
        user.setDisplayName("Market Position User");
        userService.addUser(user);

        String accessToken = jwtService.generateAccessToken(user);

        core.market.Market market = new core.market.Market("position-market", "Position Market", "Desc");
        market.resolveMarket(core.market.Outcome.NO);
        marketRepository.saveAll(java.util.List.of(market));

        tradeRepository.saveAll(java.util.List.of(
                new db.entity.TradeEntity(null, user.getUserId(), "position-market", core.market.Outcome.NO,
                        new java.math.BigDecimal("3.0"), new java.math.BigDecimal("4.50"),
                        java.sql.Timestamp.from(java.time.Instant.parse("2026-03-27T12:00:00Z"))),
                new db.entity.TradeEntity(null, user.getUserId(), "position-market", core.market.Outcome.YES,
                        new java.math.BigDecimal("2.0"), new java.math.BigDecimal("5.00"),
                        java.sql.Timestamp.from(java.time.Instant.parse("2026-03-27T11:00:00Z"))),
                new db.entity.TradeEntity(null, user.getUserId(), "position-market", core.market.Outcome.NO,
                        new java.math.BigDecimal("1.5"), new java.math.BigDecimal("2.00"),
                        java.sql.Timestamp.from(java.time.Instant.parse("2026-03-27T10:00:00Z")))));

        mockMvc.perform(get("/v1/markets/position-market/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketId").value("position-market"))
                .andExpect(jsonPath("$.marketStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedOutcome").value("NO"))
                .andExpect(jsonPath("$.yesSharesHeld").value(2.0))
                .andExpect(jsonPath("$.noSharesHeld").value(4.5))
                .andExpect(jsonPath("$.totalInvested").value(11.5))
                .andExpect(jsonPath("$.realizedPayout").value(4.5))
                .andExpect(jsonPath("$.realizedNetPnl").value(-7.0))
                .andExpect(jsonPath("$.tradeCount").value(3))
                .andExpect(jsonPath("$.trades[0].outcome").value("NO"))
                .andExpect(jsonPath("$.trades[0].cost").value(4.5));
    }
}
