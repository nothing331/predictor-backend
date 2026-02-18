package core.repository.adapter.json;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.stereotype.Repository;

import core.market.Market;
import core.repository.file.FileMarketRepository;
import core.repository.port.MarketRepository;

@Repository("marketJsonAdapter")
public class MarketJsonAdapter implements MarketRepository {

    private final FileMarketRepository fileMarketRepository;

    public MarketJsonAdapter(FileMarketRepository fileMarketRepository) {
        this.fileMarketRepository = fileMarketRepository;
    }

    @Override
    public void saveAll(Collection<Market> markets) {
        fileMarketRepository.saveAllToJson(markets);
    }

    @Override
    public Collection<Market> loadAll() {
        return fileMarketRepository.loadAllFromJson();
    }

    @Override
    public Market loadById(String marketId) {
        return fileMarketRepository.loadByIdFromJson(marketId);
    }

    @Override
    public Collection<Market> loadByStatus(String status) {
        if ("OPEN".equalsIgnoreCase(status) || "RESOLVED".equalsIgnoreCase(status)) {
            return fileMarketRepository.loadByStatusFromJson(status.toUpperCase());
        } else {
            return new ArrayList<>();
        }
    }
}
