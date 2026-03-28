package core.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

import api.dto.auth.AuthUserResponse;
import api.dto.auth.TokenResponse;
import core.analytics.AnalyticsEventNames;
import core.analytics.AnalyticsService;
import core.user.User;


@Service
public class AuthService {

    @Autowired private UserService userService;
    @Autowired private GiftService giftService;
    @Autowired private JwtService jwtService;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private GoogleIdTokenVerifier googleIdTokenVerifier;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Autowired private AnalyticsService analyticsService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${app.jwt-expiry-ms}")
    private long jwtExpiryMs;
    
    
    public TokenResponse loginWithGoogle(String idToken) { 
        GoogleIdToken googleToken;
        try {
            googleToken = googleIdTokenVerifier.verify(idToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        }
        if (googleToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        }

        // 2. Extract user info from the verified token
        GoogleIdToken.Payload payload = googleToken.getPayload();
        String googleSub = payload.getSubject();   // stable unique Google user ID
        String email     = payload.getEmail();
        String name      = (String) payload.get("name");

        // 3. Find or create user in your DB
        boolean emailVerified = payload.getEmailVerified();
        String pictureUrl = (String) payload.get("picture");
        core.user.GoogleProfile profile = new core.user.GoogleProfile(
            googleSub, email, name, pictureUrl, emailVerified
        );
        User user = userService.upsertGoogleUser(profile);

        // 4. Issue your own tokens
        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.create(user);
        identifyUser(user);
        analyticsService.capture(user.getUserId(), AnalyticsEventNames.LOGIN_SUCCEEDED, java.util.Map.of(
                "provider", "google"));

        return new TokenResponse(accessToken, refreshToken, (int) (jwtExpiryMs / 1000));
     }

    public TokenResponse demoRegister(String username, String password, String email) {
        if (userService.findByUserName(username) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        if (userService.findByEmail(email) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        User newUser = new User(java.util.UUID.randomUUID().toString());
        newUser.setDisplayName(username);
        newUser.setEmail(email);
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setEmailVerified(true); // For demo auth, assume verified or just skip it

        userService.addUser(newUser);

        String accessToken  = jwtService.generateAccessToken(newUser);
        String refreshToken = refreshTokenService.create(newUser);
        identifyUser(newUser);
        analyticsService.capture(newUser.getUserId(), AnalyticsEventNames.REGISTER_SUCCEEDED, java.util.Map.of(
                "provider", "demo"));
        return new TokenResponse(accessToken, refreshToken, (int) (jwtExpiryMs / 1000));
    }

    public TokenResponse demoLogin(String username, String password) {
        User user = userService.findByUserName(username);
        if (user == null || user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            if (user != null) {
                analyticsService.capture(user.getUserId(), AnalyticsEventNames.LOGIN_FAILED, java.util.Map.of(
                        "provider", "demo",
                        "reason", "Invalid username or password"));
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.create(user);
        identifyUser(user);
        analyticsService.capture(user.getUserId(), AnalyticsEventNames.LOGIN_SUCCEEDED, java.util.Map.of(
                "provider", "demo"));
        return new TokenResponse(accessToken, refreshToken, (int) (jwtExpiryMs / 1000));
    }


    public TokenResponse refresh(String refreshToken) {
        User user = refreshTokenService.validate(refreshToken);
        refreshTokenService.rotate(refreshToken); // invalidate old token
        String accessToken  = jwtService.generateAccessToken(user);
        String newRefresh   = refreshTokenService.create(user);
        return new TokenResponse(accessToken, newRefresh, (int) (jwtExpiryMs / 1000));
    }


    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }


    public AuthUserResponse getCurrentUser(String userId) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        GiftService.GiftStatus giftStatus = giftService.getGiftStatus(user);
        return new AuthUserResponse(
            user.getUserId(),
            user.getEmail(),
            user.getDisplayName(),
            user.getPictureUrl(),
            user.getBalance(),
            user.getRole().name(),
            giftStatus.giftAvailable(),
            giftStatus.nextGiftAt()
        );
    }

    private void identifyUser(User user) {
        java.util.Map<String, Object> properties = new java.util.LinkedHashMap<>();
        if (user.getEmail() != null) {
            properties.put("email", user.getEmail());
        }
        if (user.getDisplayName() != null) {
            properties.put("displayName", user.getDisplayName());
        }
        properties.put("role", user.getRole().name());
        properties.put("emailVerified", user.isEmailVerified());
        analyticsService.identify(user.getUserId(), properties);
    }
}
