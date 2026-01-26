package core.repository;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import core.user.User;

public class UserRepository {

    private static final String DATA_FILE_PATH = "data/user.json";
    private final ObjectMapper mapper;

    public UserRepository() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    public void saveAll(Collection<User> users) {
        try {
            // Ensure data directory exists
            File dataFile = new File(DATA_FILE_PATH);
            File dataDir = dataFile.getParentFile();
            if (dataDir != null && !dataDir.exists()) {
                dataDir.mkdirs();
            }

            // 2. Write to temp file
            File tempFile = File.createTempFile("user_temp", ".json", dataDir);
            mapper.writeValue(tempFile, users);

            // 3. Atomic move
            Files.move(tempFile.toPath(), dataFile.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            throw new RuntimeException("Failed to save users to JSON", e);
        }
    }

    public Collection<User> loadAll() {
        File dataFile = new File(DATA_FILE_PATH);
        if (!dataFile.exists()) {
            return new ArrayList<>();
        }

        try {
            // 1. Read & Parse
            List<User> users = mapper.readValue(dataFile,
                    mapper.getTypeFactory().constructCollectionType(List.class, User.class));

            return users != null ? users : new ArrayList<>();

        } catch (IOException e) {
            throw new RuntimeException("Failed to load users from JSON", e);
        }
    }
}
