package core;

import core.market.Market;
import core.settlement.SettlementEngine;
import core.market.Outcome;
import core.repository.MarketRepository;
import core.repository.TradeRepository;
import core.repository.UserRepository;
import core.service.MarketService;
import core.service.PersistenceService;
import core.service.TradeService;
import core.service.UserService;
import core.trade.Trade;
import core.trade.TradeEngine;
import core.user.Position;
import core.user.User;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main bootstrap and entry point for the Prediction Market Game.
 * Holds the in-memory state and coordinates the engines.
 */
public class PredictionMarketGame {

    // In-memory state
    private final Map<String, Market> markets = new HashMap<>();
    private final Map<String, User> users = new HashMap<>();
    private final List<Trade> trades = new ArrayList<>();

    // Services and Engines
    private final PersistenceService persistenceService;
    private final TradeEngine tradeEngine;
    private final SettlementEngine settlementEngine;

    public PredictionMarketGame() {
        // Wiring
        MarketRepository marketRepo = new MarketRepository();
        UserRepository userRepo = new UserRepository();
        TradeRepository tradeRepo = new TradeRepository();

        MarketService marketService = new MarketService(marketRepo);
        UserService userService = new UserService(userRepo);
        TradeService tradeService = new TradeService(tradeRepo);

        this.persistenceService = new PersistenceService(marketService, tradeService, userService);

        // Step 5: Load state at startup
        loadState();

        // Initialize Engines
        // We inject the persistence service into engines if they need to persist
        // directly,
        // or we handle persistence here.
        // Based on instructions, we will modify the engines to handle persistence or
        // wrap them here. Let's start with basic initialization.
        this.tradeEngine = new TradeEngine();
        this.settlementEngine = new SettlementEngine();
    }

    private void loadState() {
        System.out.println("Loading state from disk...");

        PersistenceService.LoadedState state = persistenceService.loadAll();

        // Rebuild Maps
        markets.clear();
        for (Market m : state.getMarkets()) {
            markets.put(m.getMarketId(), m);
        }

        users.clear();
        for (User u : state.getUsers()) {
            users.put(u.getUserId(), u);
        }

        trades.clear();
        trades.addAll(state.getTrades());

        // Validate References
        validateReferences();

        System.out.println("State loaded: " + markets.size() + " markets, "
                + users.size() + " users, " + trades.size() + " trades.");
    }

    private void validateReferences() {
        // Validate positions reference existing markets
        for (User user : users.values()) {
            for (Position pos : user.getPositions().values()) {
                if (!markets.containsKey(pos.getMarketId())) {
                    System.err.println("WARNING: User " + user.getUserId()
                            + " has position in unknown market " + pos.getMarketId());
                    // here we are removing the position from the user
                    user.getPositions().remove(pos.getMarketId());
                }
            }
        }

        // Validate trades reference existing users and markets
        for (Trade trade : trades) {
            if (!users.containsKey(trade.getUserId())) {
                System.err.println("WARNING: Trade references unknown user " + trade.getUserId());
                trades.remove(trade);
            }
            if (!markets.containsKey(trade.getMarketId())) {
                System.err.println("WARNING: Trade references unknown market " + trade.getMarketId());
                trades.remove(trade);
            }
        }
    }

    // Step 6: Persist after every trade and resolution
    // We will expose methods to perform actions and ensure they persist.

    public Trade executeTrade(String userId, String marketId, Outcome outcome, double sharesToBuy) {
        User user = users.get(userId);
        Market market = markets.get(marketId);

        if (user == null)
            throw new IllegalArgumentException("User not found: " + userId);
        if (market == null)
            throw new IllegalArgumentException("Market not found: " + marketId);

        // Execute via Engine
        // Note: TradeEngine is stateless, we pass the objects
        Trade trade = tradeEngine.executeTrade(user, market, outcome, sharesToBuy);

        // Update local state (TradeEngine modifies User/Market in place, but we need to
        // add Trade)
        trades.add(trade);

        // PERSIST
        // "After a successful trade, persist users, markets, and trades."
        persistenceService.saveAll(markets.values(), trades, users.values());

        return trade;
    }

    public void resolveMarket(String marketId, Outcome outcome) {
        Market market = markets.get(marketId);
        if (market == null)
            throw new IllegalArgumentException("Market not found: " + marketId);

        market.resolveMarket(outcome);

        // Settlement
        settlementEngine.settleMarket(market, users.values());

        // PERSIST
        // "After resolving a market, persist users and markets." (and trades? prompt
        // says 'persist users and markets')
        persistenceService.saveAll(markets.values(), trades, users.values());
    }

    public Map<String, Market> getMarkets() {
        return markets;
    }

    public Map<String, User> getUsers() {
        return users;
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public static void main(String[] args) {
        new PredictionMarketGame();
    }
}
