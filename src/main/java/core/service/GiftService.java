package core.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import api.dto.GiftClaimResponse;
import core.user.User;

@Service
public class GiftService {

    private static final BigDecimal GIFT_AMOUNT = new BigDecimal("500.00");
    private static final Duration CLAIM_INTERVAL = Duration.ofHours(12);

    private final UserService userService;

    public GiftService(UserService userService) {
        this.userService = userService;
    }

    public GiftStatus getGiftStatus(User user) {
        return getGiftStatus(user, Instant.now());
    }

    @Transactional
    public GiftClaimResponse claimGift(String userId) {
        User user = userService.getUserByIdForUpdate(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        Instant now = Instant.now();
        if (!isGiftAvailable(user, now)) {
            GiftStatus status = getGiftStatus(user, now);
            return new GiftClaimResponse(
                    user.getBalance(),
                    BigDecimal.ZERO,
                    false,
                    user.getLastGiftClaimedAt(),
                    status.nextGiftAt(),
                    status.giftAvailable());
        }

        user.setBalance(user.getBalance().add(GIFT_AMOUNT));
        user.setLastGiftClaimedAt(now);
        userService.saveUser(user);

        GiftStatus status = getGiftStatus(user, now);
        return new GiftClaimResponse(
                user.getBalance(),
                GIFT_AMOUNT,
                true,
                user.getLastGiftClaimedAt(),
                status.nextGiftAt(),
                status.giftAvailable());
    }

    private GiftStatus getGiftStatus(User user, Instant referenceTime) {
        Instant nextGiftAt = calculateNextGiftAt(user.getLastGiftClaimedAt());
        boolean giftAvailable = nextGiftAt == null || !referenceTime.isBefore(nextGiftAt);
        return new GiftStatus(giftAvailable, giftAvailable ? null : nextGiftAt);
    }

    private boolean isGiftAvailable(User user, Instant referenceTime) {
        return getGiftStatus(user, referenceTime).giftAvailable();
    }

    private Instant calculateNextGiftAt(Instant lastGiftClaimedAt) {
        return lastGiftClaimedAt == null ? null : lastGiftClaimedAt.plus(CLAIM_INTERVAL);
    }

    public record GiftStatus(boolean giftAvailable, Instant nextGiftAt) {
    }
}
