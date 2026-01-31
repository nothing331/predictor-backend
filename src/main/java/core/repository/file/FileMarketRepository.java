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

import core.market.Market;

@Component
public class FileMarketRepository {

    private static final String DATA_FILE_PATH = "data/markets.json";
    private final ObjectMapper mapper;

    public FileMarketRepository() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    public void saveAllToJson(Collection<Market> markets) {
        try {
            // Ensure data directory exists
            File dataFile = new File(DATA_FILE_PATH);
            File dataDir = dataFile.getParentFile();
            if (dataDir != null && !dataDir.exists()) {
                dataDir.mkdirs();
            }

            // 2. Write to temp file
            File tempFile = File.createTempFile("market_temp", ".json", dataDir);
            mapper.writeValue(tempFile, markets);

            // 3. Atomic move
            Files.move(tempFile.toPath(), dataFile.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            throw new RuntimeException("Failed to save markets to JSON", e);
        }
    }

    public Collection<Market> loadAllFromJson() {
        File dataFile = new File(DATA_FILE_PATH);
        if (!dataFile.exists()) {
            return new ArrayList<>();
        }

        try {
            // 1. Read & Parse
            List<Market> markets = mapper.readValue(dataFile,
                    mapper.getTypeFactory().constructCollectionType(List.class, Market.class));

            return markets != null ? markets : new ArrayList<>();

        } catch (IOException e) {
            throw new RuntimeException("Failed to load markets from JSON", e);
        }
    }
}