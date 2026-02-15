package core.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import api.dto.GetUsersRequest;
import core.user.User;
import core.user.NewUser;
import core.user.Position;
import core.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public void saveAll(Collection<User> users) {
        if (users != null) {
            for (User user : users) {
                validateUser(user);
            }
        }
        repository.saveAll(users);
    }

    public Collection<User> loadAll() {
        Collection<User> users = repository.loadAll();
        if (users != null) {
            for (User user : users) {
                validateUser(user);
            }
        }
        return users;
    }

    public boolean addUser(NewUser newUser) {
        Collection<User> storedUsers = loadAll();

        boolean exists = storedUsers.stream()
                .anyMatch(u -> u.getUserId().equalsIgnoreCase(newUser.getUserId()));

        if (exists) {
            return false;
        }

        User user = new User(newUser.getUserId());
        storedUsers.add(user);

        saveAll(storedUsers);
        return true;
    }

    public List<GetUsersRequest> getAllUsers() {
        Collection<User> users = loadAll();
        List<GetUsersRequest> response = users.stream()
                .map(user -> new GetUsersRequest(user.getUserId()))
                .toList();
        return response;
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        // Validate userId is present
        if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            throw new IllegalStateException("User userId cannot be null or empty");
        }

        // Validate balance is not null
        if (user.getBalance() == null) {
            throw new IllegalStateException("User balance cannot be null for user: " + user.getUserId());
        }

        // Validate positions map structure: marketId -> Position
        Map<String, Position> positions = user.getPositions();
        if (positions != null) {
            for (Map.Entry<String, Position> entry : positions.entrySet()) {
                String marketId = entry.getKey();
                Position position = entry.getValue();

                // Validate marketId is not null or empty
                if (marketId == null || marketId.trim().isEmpty()) {
                    throw new IllegalStateException(
                            "Position marketId cannot be null or empty for user: " + user.getUserId());
                }

                // Validate position is not null
                if (position == null) {
                    throw new IllegalStateException(
                            "Position cannot be null for marketId: " + marketId + " in user: " + user.getUserId());
                }

                // Validate that the position's marketId matches the map key
                if (!marketId.equals(position.getMarketId())) {
                    throw new IllegalStateException("Position marketId mismatch: map key is " + marketId +
                            " but position.getMarketId() is " + position.getMarketId() + " for user: "
                            + user.getUserId());
                }
            }
        }
    }
}
