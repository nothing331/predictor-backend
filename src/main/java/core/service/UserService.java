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

import core.store.UserStore;

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

    public boolean addUser(NewUser newUser) {
        Collection<User> storedUsers = userStore.getAll();

        boolean exists = storedUsers.stream()
                .anyMatch(u -> u.getUserId().equalsIgnoreCase(newUser.getUserId()));

        if (exists) {
            return false;
        }

        User user = new User(newUser.getUserId());
        userStore.put(user);

        saveAll(userStore.getAll());
        return true;
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
}
