package core.repository.adapter.dual;

import java.math.BigDecimal;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.stereotype.Repository;

import core.trade.Trade;
import core.repository.port.TradeRepository;

@Repository("tradeDualAdapter")

public class DualWriteTradeAdapter implements TradeRepository {

    private final TradeRepository jsonAdapter;
    private final TradeRepository dbAdapter;

    public DualWriteTradeAdapter(
            @Qualifier("tradeJsonAdapter") TradeRepository jsonAdapter,
            @Qualifier("tradeDbAdapter") TradeRepository dbAdapter) {
        this.jsonAdapter = jsonAdapter;
        this.dbAdapter = dbAdapter;
    }

    @Override
    public void saveAll(Collection<Trade> trades) {
        dbAdapter.saveAll(trades);
        jsonAdapter.saveAll(trades);
    }

    @Override
    public Collection<Trade> loadAll() {
        return dbAdapter.loadAll();
    }

    @Override
    public Collection<Trade> loadByMarketId(String marketId) {
        return dbAdapter.loadByMarketId(marketId);
    }

    @Override
    public Collection<Trade> loadByMarketIdOrdered(String marketId) {
        return dbAdapter.loadByMarketIdOrdered(marketId);
    }

    @Override
    public BigDecimal sumCostByMarketId(String marketId) {
        return dbAdapter.sumCostByMarketId(marketId);
    }
}
