package core.repository.adapter.json;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.stream.Collectors;

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

    @Override
    public Collection<Trade> loadByMarketId(String marketId) {
        return fileTradeRepository.loadAllFromJson().stream()
                .filter(t -> marketId.equals(t.getMarketId()))
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal sumCostByMarketId(String marketId) {
        return fileTradeRepository.loadAllFromJson().stream()
                .filter(t -> marketId.equals(t.getMarketId()))
                .map(core.trade.Trade::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
