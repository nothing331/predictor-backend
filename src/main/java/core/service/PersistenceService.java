package core.service;

import java.util.Collection;

import core.market.Market;
import core.trade.Trade;
import core.user.User;

public class PersistenceService {
    private final MarketService marketService;
    private final TradeService tradeService;
    private final UserService userService;

    public PersistenceService(MarketService marketService, TradeService tradeService, UserService userService) {
        this.marketService = marketService;
        this.tradeService = tradeService;
        this.userService = userService;
    }

    public MarketService getMarketService() {
        return marketService;
    }

    public TradeService getTradeService() {
        return tradeService;
    }

    public UserService getUserService() {
        return userService;
    }

    public void saveAll(Collection<Market> markets, Collection<Trade> trades, Collection<User> users) {
        marketService.saveAll(markets);
        tradeService.saveAll(trades);
        userService.saveAll(users);
    }

    public LoadedState loadAll() {
        return new LoadedState(
                marketService.loadAll(),
                tradeService.loadAll(),
                userService.loadAll());
    }

    public static class LoadedState {
        private final Collection<Market> markets;
        private final Collection<Trade> trades;
        private final Collection<User> users;

        public LoadedState(Collection<Market> markets, Collection<Trade> trades, Collection<User> users) {
            this.markets = markets;
            this.trades = trades;
            this.users = users;
        }

        public Collection<Market> getMarkets() {
            return markets;
        }

        public Collection<Trade> getTrades() {
            return trades;
        }

        public Collection<User> getUsers() {
            return users;
        }
    }
}
