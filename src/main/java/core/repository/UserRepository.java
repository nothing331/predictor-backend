package core.repository;

import java.util.Collection;

import core.user.User;
import core.repository.file.FileUserRepository;

public class UserRepository {

    public UserRepository() {
    }

    public void saveAll(Collection<User> users) {
        FileUserRepository fileUserRepository = new FileUserRepository();
        fileUserRepository.saveAllToJson(users);
    }

    public Collection<User> loadAll() {
        FileUserRepository fileUserRepository = new FileUserRepository();
        return fileUserRepository.loadAllFromJson();
    }
}
