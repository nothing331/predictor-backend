package core.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import api.dto.GetUsersRequest;
import core.user.User;
import core.user.Position;
import core.repository.port.UserRepository;
import core.store.UserStore;
import core.user.User;
import core.user.GoogleProfile;

@Service
public class UserService {
    private final UserRepository repository;
    private final UserStore userStore;

    public UserService(UserRepository repository, UserStore userStore) {
        this.repository = repository;
        this.userStore = userStore;
    }

    public void saveAll(Collection<User> users) {
        if (users != null) {
            for (User user : users) {
                user.validate();
                userStore.put(user);
            }
        }
        repository.saveAll(userStore.getAll());
    }

    public Collection<User> loadAll() {
        return userStore.getAll();
    }

    public User addUser(User user) {
        Collection<User> storedUsers = userStore.getAll();

        boolean exists = storedUsers.stream()
                .anyMatch(u -> u.getUserId().equalsIgnoreCase(user.getUserId()));

        if (exists) {
            return userStore.get(user.getUserId());
        }

        userStore.put(user);
        saveAll(userStore.getAll());
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
        saveAll(userStore.getAll());
    }

    public User getUserById(String userId) {
        return userStore.get(userId);
    }

    public User findByEmail(String email) {
        // First check store (cache)
        User cached = loadAll().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst()
                .orElse(null);
        if (cached != null) return cached;
        
        // Then check repository
        return repository.loadByEmail(email);
    }

    public User findByUserName(String userName) {
        // First check store (cache)
        User cached = loadAll().stream()
                .filter(u -> userName.equalsIgnoreCase(u.getDisplayName()))
                .findFirst()
                .orElse(null);
        if (cached != null) return cached;

        // Then check repository
        return repository.loadByUserName(userName);
    }

    public User upsertGoogleUser(GoogleProfile profile) {
        Collection<User> storedUsers = userStore.getAll();
        User existingUser = storedUsers.stream()
                .filter(u -> profile.googleSub().equals(u.getGoogleSub()))
                .findFirst()
                .orElse(null);

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
