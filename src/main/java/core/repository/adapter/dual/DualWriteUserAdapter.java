package core.repository.adapter.dual;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.stereotype.Repository;

import core.user.User;
import core.repository.port.UserRepository;

@Repository("userDualAdapter")

public class DualWriteUserAdapter implements UserRepository {

    private final UserRepository jsonAdapter;
    private final UserRepository dbAdapter;

    public DualWriteUserAdapter(
            @Qualifier("userJsonAdapter") UserRepository jsonAdapter,
            @Qualifier("userDbAdapter") UserRepository dbAdapter) {
        this.jsonAdapter = jsonAdapter;
        this.dbAdapter = dbAdapter;
    }

    @Override
    public void saveAll(Collection<User> users) {
        dbAdapter.saveAll(users);
        jsonAdapter.saveAll(users);
    }

    @Override
    public Collection<User> loadAll() {
        return dbAdapter.loadAll();
    }

    @Override
    public User loadById(String userId) {
        return dbAdapter.loadById(userId);
    }
}
