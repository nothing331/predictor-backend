package core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

import api.dto.auth.TokenResponse;
import core.analytics.AnalyticsEventNames;
import core.analytics.AnalyticsService;
import core.user.User;

class AuthServiceAnalyticsTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private AnalyticsService analyticsService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService();
        ReflectionTestUtils.setField(authService, "userService", userService);
        ReflectionTestUtils.setField(authService, "jwtService", jwtService);
        ReflectionTestUtils.setField(authService, "refreshTokenService", refreshTokenService);
        ReflectionTestUtils.setField(authService, "googleIdTokenVerifier", googleIdTokenVerifier);
        ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(authService, "analyticsService", analyticsService);
        ReflectionTestUtils.setField(authService, "jwtExpiryMs", 900000L);
    }

    @Test
    void googleLoginTracksIdentifyAndSuccessEvent() throws Exception {
        User user = new User("sub_123");
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");

        when(googleIdTokenVerifier.verify("valid-token"))
                .thenReturn(createMockGoogleToken("sub_123", "test@example.com", "Test User"));
        when(userService.upsertGoogleUser(any())).thenReturn(user);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.create(user)).thenReturn("refresh-token");

        TokenResponse response = authService.loginWithGoogle("valid-token");

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        verify(analyticsService).identify(eq("sub_123"), argThat(props ->
                "test@example.com".equals(props.get("email"))
                        && "Test User".equals(props.get("displayName"))
                        && "USER".equals(props.get("role"))));
        verify(analyticsService).capture(eq("sub_123"), eq(AnalyticsEventNames.LOGIN_SUCCEEDED), argThat(props ->
                "google".equals(props.get("provider"))));
    }

    @Test
    void demoRegisterTracksIdentifyAndRegisterSuccess() {
        when(userService.findByUserName("demo_user")).thenReturn(null);
        when(userService.findByEmail("demo@example.com")).thenReturn(null);
        when(passwordEncoder.encode("demo_pass")).thenReturn("hashed-password");
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(refreshTokenService.create(any(User.class))).thenReturn("refresh-token");

        TokenResponse response = authService.demoRegister("demo_user", "demo_pass", "demo@example.com");

        assertEquals("access-token", response.accessToken());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).addUser(userCaptor.capture());
        verify(analyticsService).identify(eq(userCaptor.getValue().getUserId()), argThat(props ->
                "demo@example.com".equals(props.get("email"))
                        && "demo_user".equals(props.get("displayName"))));
        verify(analyticsService).capture(eq(userCaptor.getValue().getUserId()),
                eq(AnalyticsEventNames.REGISTER_SUCCEEDED),
                argThat(props -> "demo".equals(props.get("provider"))));
    }

    @Test
    void demoLoginFailureTracksExistingUser() {
        User user = new User("user-1");
        user.setDisplayName("demo_user");
        user.setPasswordHash("hashed-password");

        when(userService.findByUserName("demo_user")).thenReturn(user);
        when(passwordEncoder.matches("wrong-pass", "hashed-password")).thenReturn(false);

        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> authService.demoLogin("demo_user", "wrong-pass"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(analyticsService).capture(eq("user-1"), eq(AnalyticsEventNames.LOGIN_FAILED), argThat(props ->
                "demo".equals(props.get("provider"))
                        && "Invalid username or password".equals(props.get("reason"))));
    }

    private GoogleIdToken createMockGoogleToken(String sub, String email, String name) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject(sub);
        payload.setEmail(email);
        payload.set("name", name);
        payload.setEmailVerified(true);
        payload.set("picture", "http://example.com/pic.jpg");

        com.google.api.client.json.webtoken.JsonWebSignature.Header header =
                new com.google.api.client.json.webtoken.JsonWebSignature.Header();
        header.setAlgorithm("RS256");

        try {
            return new GoogleIdToken(header, payload, new byte[0], new byte[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
