package core.service;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import api.dto.GetUsersRequest;
import core.repository.port.UserRepository;
import core.user.GoogleProfile;
import core.user.User;

/**
 * User read/write service. After step 6 of the async-settlement refactor,
 * the in-memory {@code UserStore} is gone and every read goes to Postgres.
 * See {@code docs/adr/0001-remove-in-memory-stores-from-correctness-paths.md}.
 */
@Service
public class UserService {
    private final UserRepository repository;
    private final LedgerService ledgerService;

    public UserService(UserRepository repository, LedgerService ledgerService) {
        this.repository = repository;
        this.ledgerService = ledgerService;
    }

    public void saveAll(Collection<User> users) {
        if (users == null) {
            return;
        }
        for (User user : users) {
            user.validate();
        }
        repository.saveAll(users);
    }

    public Collection<User> loadAll() {
        return repository.loadAll();
    }

    @Transactional
    public User addUser(User user) {
        User existing = repository.loadById(user.getUserId());
        if (existing != null) {
            return existing;
        }

        repository.saveAll(List.of(user));
        ledgerService.recordStartingBalance(user);
        return user;
    }

    public List<GetUsersRequest> getAllUsers() {
        return loadAll().stream()
                .map(user -> new GetUsersRequest(user.getUserId()))
                .toList();
    }

    public void saveUser(User user) {
        user.validate();
        repository.saveAll(List.of(user));
    }

    public User getUserById(String userId) {
        return repository.loadById(userId);
    }

    public User getUserByIdForUpdate(String userId) {
        return repository.loadByIdForUpdate(userId);
    }

    public User findByEmail(String email) {
        return repository.loadByEmail(email);
    }

    public User findByUserName(String userName) {
        return repository.loadByUserName(userName);
    }

    public User upsertGoogleUser(GoogleProfile profile) {
        User existingUser = getUserById(profile.googleSub());

        if (existingUser != null) {
            existingUser.setGoogleSub(profile.googleSub());
            existingUser.setEmail(profile.email());
            existingUser.setDisplayName(profile.name());
            existingUser.setPictureUrl(profile.pictureUrl());
            existingUser.setEmailVerified(profile.emailVerified());
            saveUser(existingUser);
            return existingUser;
        }

        User newUser = new User(profile.googleSub());
        newUser.setEmail(profile.email());
        newUser.setDisplayName(profile.name());
        newUser.setPictureUrl(profile.pictureUrl());
        newUser.setGoogleSub(profile.googleSub());
        newUser.setEmailVerified(profile.emailVerified());
        return addUser(newUser);
    }
}
