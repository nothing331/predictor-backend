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
    private final LedgerService ledgerService;

    public GiftService(UserService userService, LedgerService ledgerService) {
        this.userService = userService;
        this.ledgerService = ledgerService;
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
        Instant giftWindowStart = currentGiftWindowStart(now);
        if (!isGiftAvailable(user, now)) {
            GiftStatus status = getGiftStatus(user, now);
            return new GiftClaimResponse(
                    user.getBalance(),
                    GIFT_AMOUNT,
                    true,
                    user.getLastGiftClaimedAt(),
                    status.nextGiftAt(),
                    status.giftAvailable());
        }

        // Encode the cadence into the key so that changing CLAIM_INTERVAL in the
        // future does not collide with legacy keys: "43200:1747267200" = 12h window
        // starting at epoch 1747267200.
        String referenceKey = CLAIM_INTERVAL.toSeconds() + ":" + giftWindowStart.getEpochSecond();
        ledgerService.recordGiftCredit(GIFT_AMOUNT, user, referenceKey);

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
        Instant currentWindowStart = currentGiftWindowStart(referenceTime);
        Instant lastClaimedAt = user.getLastGiftClaimedAt();
        boolean giftAvailable = lastClaimedAt == null || lastClaimedAt.isBefore(currentWindowStart);
        Instant nextGiftAt = giftAvailable ? null : currentWindowStart.plus(CLAIM_INTERVAL);
        return new GiftStatus(giftAvailable, giftAvailable ? null : nextGiftAt);
    }

    private boolean isGiftAvailable(User user, Instant referenceTime) {
        return getGiftStatus(user, referenceTime).giftAvailable();
    }

    private Instant currentGiftWindowStart(Instant now) {
        long seconds = now.getEpochSecond();
        long window = CLAIM_INTERVAL.toSeconds();
        return Instant.ofEpochSecond((seconds / window) * window);
    }

    public record GiftStatus(boolean giftAvailable, Instant nextGiftAt) {
    }
}
