package core.repository.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import core.user.User;

@Component
public class FileUserRepository {
    private static final String DATA_FILE_PATH = "data/users.json";
    private final ObjectMapper mapper;

    public FileUserRepository() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    public void saveAllToJson(Collection<User> users) {
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

    public Collection<User> loadAllFromJson() {
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
