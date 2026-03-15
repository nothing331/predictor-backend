package core.repository.adapter.json;

import java.util.Collection;

import org.springframework.stereotype.Repository;

import core.user.User;
import core.repository.file.FileUserRepository;
import core.repository.port.UserRepository;

@Repository("userJsonAdapter")
public class UserJsonAdapter implements UserRepository {

    private final FileUserRepository fileUserRepository;

    public UserJsonAdapter(FileUserRepository fileUserRepository) {
        this.fileUserRepository = fileUserRepository;
    }

    @Override
    public void saveAll(Collection<User> users) {
        fileUserRepository.saveAllToJson(users);
    }

    @Override
    public Collection<User> loadAll() {
        return fileUserRepository.loadAllFromJson();
    }

    @Override
    public User loadById(String userId) {
        return fileUserRepository.loadByIdFromJson(userId);
    }

    @Override
    public User loadByEmail(String email) {
        return fileUserRepository.loadAllFromJson().stream()
                .filter(u -> email.equals(u.getEmail()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public User loadByUserName(String userName) {
        return fileUserRepository.loadAllFromJson().stream()
                .filter(u -> userName.equals(u.getDisplayName()))
                .findFirst()
                .orElse(null);
    }
}
