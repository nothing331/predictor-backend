package core.store;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import core.user.User;
import core.repository.port.UserRepository;

@Component
public class UserStore {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final UserRepository repository;

    public UserStore(UserRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        Collection<User> loaded = repository.loadAll();
        if (loaded != null) {
            loaded.forEach(u -> {
                u.validate();
                users.put(u.getUserId(), u);
            });
        }
    }

    public User get(String id) {
        return users.get(id);
    }

    public void put(User user) {
        users.put(user.getUserId(), user);
    }

    public Collection<User> getAll() {
        return users.values();
    }
}
