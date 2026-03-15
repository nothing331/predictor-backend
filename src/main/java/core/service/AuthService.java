package core.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.util.Collections;

import api.dto.auth.AuthUserResponse;
import api.dto.auth.TokenResponse;
import core.user.User;


@Service
public class AuthService {

    @Autowired private UserService userService;
    @Autowired private JwtService jwtService;
    @Autowired private RefreshTokenService refreshTokenService;

    @Value("${google.client-id}")
    private String clientId;
    
    
    public TokenResponse loginWithGoogle(String idToken) { 
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(clientId))
            .build();

        GoogleIdToken googleToken;
        try {
            googleToken = verifier.verify(idToken);
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

        return new TokenResponse(accessToken, refreshToken, 900);
     }


    public TokenResponse refresh(String refreshToken) {
        User user = refreshTokenService.validate(refreshToken);
        refreshTokenService.rotate(refreshToken); // invalidate old token
        String accessToken  = jwtService.generateAccessToken(user);
        String newRefresh   = refreshTokenService.create(user);
        return new TokenResponse(accessToken, newRefresh, 900);
    }


    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }


    public AuthUserResponse getCurrentUser(String userId) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return new AuthUserResponse(
            user.getUserId(),
            user.getEmail(),
            user.getDisplayName(),
            user.getPictureUrl(),
            user.getBalance()
        );
    }
}
