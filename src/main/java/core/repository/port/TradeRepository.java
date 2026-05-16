package core.repository.port;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import core.trade.Trade;

public interface TradeRepository {
    Trade save(Trade trade);

    void saveAll(Collection<Trade> trades);

    Collection<Trade> loadAll();

    Collection<Trade> loadByMarketId(String marketId);

    Collection<Trade> loadByMarketIdOrdered(String marketId);

    Collection<Trade> loadByUserIdOrderedDesc(String userId);

    Collection<Trade> loadByUserIdAndMarketIdOrderedDesc(String userId, String marketId);

    Optional<Trade> loadByUserIdAndClientRequestId(String userId, String clientRequestId);

    BigDecimal sumCostByMarketId(String marketId);
}
