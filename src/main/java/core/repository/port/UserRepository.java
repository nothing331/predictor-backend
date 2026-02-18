package core.repository.port;

import java.util.Collection;
import core.user.User;

public interface UserRepository {
    void saveAll(Collection<User> users);

    Collection<User> loadAll();

    User loadById(String userId);
}
