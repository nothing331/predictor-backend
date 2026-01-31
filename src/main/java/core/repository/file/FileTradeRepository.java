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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import core.trade.Trade;

@Component
public class FileTradeRepository {
    private static final String DATA_FILE_PATH = "data/trades.json";
    private final ObjectMapper mapper;

    public FileTradeRepository() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    public void saveAllToJson(Collection<Trade> trades) {
        try {
            // Ensure data directory exists
            File dataFile = new File(DATA_FILE_PATH);
            File dataDir = dataFile.getParentFile();
            if (dataDir != null && !dataDir.exists()) {
                dataDir.mkdirs();
            }

            // 2. Write to temp file
            File tempFile = File.createTempFile("trade_temp", ".json", dataDir);
            mapper.writeValue(tempFile, trades);

            // 3. Atomic move
            Files.move(tempFile.toPath(), dataFile.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            throw new RuntimeException("Failed to save trades to JSON", e);
        }
    }

    public Collection<Trade> loadAllFromJson() {
        File dataFile = new File(DATA_FILE_PATH);
        if (!dataFile.exists()) {
            return new ArrayList<>();
        }

        try {
            // 1. Read & Parse
            List<Trade> trades = mapper.readValue(dataFile,
                    mapper.getTypeFactory().constructCollectionType(List.class, Trade.class));

            return trades != null ? trades : new ArrayList<>();

        } catch (IOException e) {
            throw new RuntimeException("Failed to load trades from JSON", e);
        }
    }
}
