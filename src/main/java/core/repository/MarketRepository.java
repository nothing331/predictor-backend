package core.repository;

import java.util.Collection;

import core.market.Market;
import core.repository.file.FileMarketRepository;

public class MarketRepository {

    public void saveAll(Collection<Market> markets) {
        FileMarketRepository fileMarketRepository = new FileMarketRepository();
        fileMarketRepository.saveAllToJson(markets);
    }

    public Collection<Market> loadAll() {
        FileMarketRepository fileMarketRepository = new FileMarketRepository();
        return fileMarketRepository.loadAllFromJson();
    }
}
