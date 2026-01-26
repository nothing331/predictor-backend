package core.repository;

import java.util.Collection;

import core.trade.Trade;
import core.repository.file.FileTradeRepository;

public class TradeRepository {

    public TradeRepository() {
    }

    public void saveAll(Collection<Trade> trades) {
        FileTradeRepository fileTradeRepository = new FileTradeRepository();
        fileTradeRepository.saveAllToJson(trades);
    }

    public Collection<Trade> loadAll() {
        FileTradeRepository fileTradeRepository = new FileTradeRepository();
        return fileTradeRepository.loadAllFromJson();
    }
}
