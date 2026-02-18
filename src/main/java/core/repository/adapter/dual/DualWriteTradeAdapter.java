package core.repository.adapter.dual;

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
}
