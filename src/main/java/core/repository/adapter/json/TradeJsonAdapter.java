package core.repository.adapter.json;

import java.util.Collection;

import org.springframework.stereotype.Repository;

import core.trade.Trade;
import core.repository.file.FileTradeRepository;
import core.repository.port.TradeRepository;

@Repository("tradeJsonAdapter")
public class TradeJsonAdapter implements TradeRepository {

    private final FileTradeRepository fileTradeRepository;

    public TradeJsonAdapter(FileTradeRepository fileTradeRepository) {
        this.fileTradeRepository = fileTradeRepository;
    }

    @Override
    public void saveAll(Collection<Trade> trades) {
        fileTradeRepository.saveAllToJson(trades);
    }

    @Override
    public Collection<Trade> loadAll() {
        return fileTradeRepository.loadAllFromJson();
    }
}
