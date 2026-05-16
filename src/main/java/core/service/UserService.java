package core.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import api.dto.GetUsersRequest;
import core.repository.port.UserRepository;
import core.store.UserStore;
import core.user.GoogleProfile;
import core.user.User;

@Service
public class UserService {
    private final UserRepository repository;
    private final UserStore userStore;
    private final LedgerService ledgerService;

    public UserService(UserRepository repository, UserStore userStore, LedgerService ledgerService) {
        this.repository = repository;
        this.userStore = userStore;
        this.ledgerService = ledgerService;
    }

    public void saveAll(Collection<User> users) {
        if (users == null) {
            return;
        }

        List<User> usersToSave = new ArrayList<>();
        for (User user : users) {
            user.validate();
            userStore.put(user);
            usersToSave.add(user);
        }

        repository.saveAll(usersToSave);
    }

    public Collection<User> loadAll() {
        return userStore.getAll();
    }

    @Transactional
    public User addUser(User user) {
        Collection<User> storedUsers = userStore.getAll();

        boolean exists = storedUsers.stream()
                .anyMatch(u -> u.getUserId().equalsIgnoreCase(user.getUserId()));

        if (exists) {
            return userStore.get(user.getUserId());
        }

        userStore.put(user);
        repository.saveAll(List.of(user));
        ledgerService.recordStartingBalance(user);
        return user;
    }

    public List<GetUsersRequest> getAllUsers() {
        Collection<User> users = loadAll();
        List<GetUsersRequest> response = users.stream()
                .map(user -> new GetUsersRequest(user.getUserId()))
                .toList();
        return response;
    }

    public void saveUser(User user) {
        user.validate();
        userStore.put(user);
        repository.saveAll(List.of(user));
    }

    public User getUserById(String userId) {
        User persisted = repository.loadById(userId);
        if (persisted != null) {
            userStore.put(persisted);
            return persisted;
        }
        return userStore.get(userId);
    }

    public User getUserByIdForUpdate(String userId) {
        User persisted = repository.loadByIdForUpdate(userId);
        if (persisted != null) {
            userStore.put(persisted);
            return persisted;
        }
        return null;
    }

    public User findByEmail(String email) {
        User persisted = repository.loadByEmail(email);
        if (persisted != null) {
            userStore.put(persisted);
            return persisted;
        }

        return loadAll().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst()
                .orElse(null);
    }

    public User findByUserName(String userName) {
        User persisted = repository.loadByUserName(userName);
        if (persisted != null) {
            userStore.put(persisted);
            return persisted;
        }

        return loadAll().stream()
                .filter(u -> userName.equalsIgnoreCase(u.getDisplayName()))
                .findFirst()
                .orElse(null);
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
        } else {
            User newUser = new User(profile.googleSub());
            newUser.setEmail(profile.email());
            newUser.setDisplayName(profile.name());
            newUser.setPictureUrl(profile.pictureUrl());
            newUser.setGoogleSub(profile.googleSub());
            newUser.setEmailVerified(profile.emailVerified());
            return addUser(newUser);
        }
    }
}
