package core.repository.port;

import java.util.Collection;
import core.trade.Trade;

public interface TradeRepository {
    void saveAll(Collection<Trade> trades);

    Collection<Trade> loadAll();
}
