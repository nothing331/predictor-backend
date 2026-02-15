package core.repository;

import java.util.*;

import org.springframework.stereotype.Repository;

import core.market.Market;
import core.repository.file.FileMarketRepository;

@Repository
public class MarketRepository {

    private final FileMarketRepository fileMarketRepository;

    public MarketRepository(FileMarketRepository fileMarketRepository) {
        this.fileMarketRepository = fileMarketRepository;
    }

    public void saveAll(Collection<Market> markets) {
        fileMarketRepository.saveAllToJson(markets);
    }

    public Collection<Market> loadAll() {
        return fileMarketRepository.loadAllFromJson();
    }

    public Market loadById(String marketId) {
        return fileMarketRepository.loadByIdFromJson(marketId);
    }

    public Collection<Market> loadByStatus(String status) {
        if ("OPEN".equalsIgnoreCase(status) || "RESOLVED".equalsIgnoreCase(status)) {
            return fileMarketRepository.loadByStatusFromJson(status.toUpperCase());
        } else {
            return new ArrayList<>();
        }
    }
}
