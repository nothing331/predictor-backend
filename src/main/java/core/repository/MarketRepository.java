package core.repository;

import java.util.Collection;

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
}
