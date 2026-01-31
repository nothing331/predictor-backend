package core.repository;

import java.util.Collection;

import org.springframework.stereotype.Repository;

import core.trade.Trade;
import core.repository.file.FileTradeRepository;

@Repository
public class TradeRepository {

    private final FileTradeRepository fileTradeRepository;

    public TradeRepository(FileTradeRepository fileTradeRepository) {
        this.fileTradeRepository = fileTradeRepository;
    }

    public void saveAll(Collection<Trade> trades) {
        fileTradeRepository.saveAllToJson(trades);
    }

    public Collection<Trade> loadAll() {
        return fileTradeRepository.loadAllFromJson();
    }
}
