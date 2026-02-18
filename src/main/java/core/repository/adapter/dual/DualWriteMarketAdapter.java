package core.repository.adapter.dual;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.stereotype.Repository;

import core.market.Market;
import core.repository.port.MarketRepository;

@Repository("marketDualAdapter")

public class DualWriteMarketAdapter implements MarketRepository {

    private final MarketRepository jsonAdapter;
    private final MarketRepository dbAdapter;

    public DualWriteMarketAdapter(
            @Qualifier("marketJsonAdapter") MarketRepository jsonAdapter,
            @Qualifier("marketDbAdapter") MarketRepository dbAdapter) {
        this.jsonAdapter = jsonAdapter;
        this.dbAdapter = dbAdapter;
    }

    @Override
    public void saveAll(Collection<Market> markets) {
        try {
            dbAdapter.saveAll(markets);
        } catch (Exception e) {
            // Log error but continue? Or fail?
            // "Dual-write" usually implies both must succeed or at least one is primary.
            // If DB fails, should JSON fail?
            // User requested "writes to DB + JSON".
            // Let's optimize for safety: write to both. If DB fails, maybe we still want
            // JSON.
            // But if we read from DB, we need DB to succeed.
            // So DB is critical.
            throw e;
        }
        jsonAdapter.saveAll(markets);
    }

    @Override
    public Collection<Market> loadAll() {
        return dbAdapter.loadAll();
    }

    @Override
    public Market loadById(String marketId) {
        return dbAdapter.loadById(marketId);
    }

    @Override
    public Collection<Market> loadByStatus(String status) {
        return dbAdapter.loadByStatus(status);
    }
}
