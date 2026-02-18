package core.repository.port;

import java.util.Collection;
import core.market.Market;

public interface MarketRepository {
    void saveAll(Collection<Market> markets);

    Collection<Market> loadAll();

    Market loadById(String marketId);

    Collection<Market> loadByStatus(String status);
}
