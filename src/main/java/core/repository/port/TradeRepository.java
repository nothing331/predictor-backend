package core.repository.port;

import java.math.BigDecimal;
import java.util.Collection;
import core.trade.Trade;

public interface TradeRepository {
    void saveAll(Collection<Trade> trades);

    Collection<Trade> loadAll();

    Collection<Trade> loadByMarketId(String marketId);

    BigDecimal sumCostByMarketId(String marketId);
}
