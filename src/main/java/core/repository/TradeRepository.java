package core.repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import core.trade.Trade;

public class TradeRepository {

    private static final String DATA_FILE_PATH = "data/trade.json";
    private final ObjectMapper mapper;

    public TradeRepository() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    public void saveAll(Collection<Trade> trades) {
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

    public Collection<Trade> loadAll() {
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

//