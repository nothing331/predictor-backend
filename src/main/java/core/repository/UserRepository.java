package core.repository;

import java.util.Collection;

import org.springframework.stereotype.Repository;

import core.user.User;
import core.repository.file.FileUserRepository;

@Repository
public class UserRepository {

    private final FileUserRepository fileUserRepository;

    public UserRepository(FileUserRepository fileUserRepository) {
        this.fileUserRepository = fileUserRepository;
    }

    public void saveAll(Collection<User> users) {
        fileUserRepository.saveAllToJson(users);
    }

    public Collection<User> loadAll() {
        return fileUserRepository.loadAllFromJson();
    }
}
