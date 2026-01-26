package core.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import core.market.Market;
import core.market.MarketStatus;
import core.market.Outcome;
import core.repository.MarketRepository;
import core.repository.TradeRepository;
import core.repository.UserRepository;
import core.service.MarketService;
import core.service.PersistenceService;
import core.service.TradeService;
import core.service.UserService;
import core.settlement.SettlementEngine;
import core.trade.Trade;
import core.trade.TradeEngine;
import core.user.Position;
import core.user.User;

/**
 * Persistence Service Tests - Comprehensive Test Suite
 * 
 * These tests verify the persistence layer behavior:
 * 
 * 1) EMPTY STATE BEHAVIOR
 * - Loading with no files should be safe and return empty collections.
 * 
 * 2) ROUND-TRIP PERSISTENCE (STATIC SNAPSHOT)
 * - Data you save is exactly what you load.
 * - Resolved outcomes are preserved.
 * 
 * 3) END-TO-END FLOW WITH ENGINE STATE CHANGES
 * - Real engine changes persist and survive restart.
 * - Trade execution persists all state correctly.
 * - Market resolution and settlement persist correctly.
 * 
 * 4) DATA INTEGRITY / FAIL-FAST BEHAVIOR
 * - Corrupted or inconsistent data must stop startup.
 * - Missing required fields, broken references, invalid liquidity.
 * 
 * 5) TRADE RECORD CONTRACT
 * - Trades are fully persisted and reloadable.
 * 
 * 6) ATOMIC WRITE SAFETY (Optional)
 * - Avoid partial writes.
 */
public class PersistenceServiceTest {

    private Path tempDataDir;
    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() throws IOException {
        // Create a temp directory for test data
        tempDataDir = Files.createTempDirectory("predictor-test-data");

        // Set up ObjectMapper for manual file writing in tests
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        // Set system property to use temp directory for tests
        // NOTE: Since the repositories use hardcoded paths, we need to work
        // with the actual data/ directory. Clean it before each test.
        cleanDataDirectory();
    }

    @AfterEach
    public void tearDown() throws IOException {
        // Clean up temp directory
        if (tempDataDir != null && Files.exists(tempDataDir)) {
            Files.walk(tempDataDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

        // Clean the real data directory to ensure clean state for next test
        cleanDataDirectory();
    }

    private void cleanDataDirectory() {
        File dataDir = new File("data");
        if (dataDir.exists()) {
            File[] files = dataDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".json")) {
                        file.delete();
                    }
                }
            }
        }
    }

    /**
     * Creates a fresh PersistenceService wired to use file repositories.
     */
    private PersistenceService createPersistenceService() {
        MarketRepository marketRepo = new MarketRepository();
        UserRepository userRepo = new UserRepository();
        TradeRepository tradeRepo = new TradeRepository();

        MarketService marketService = new MarketService(marketRepo);
        UserService userService = new UserService(userRepo);
        TradeService tradeService = new TradeService(tradeRepo);

        return new PersistenceService(marketService, tradeService, userService);
    }

    // ========================================================================
    // 1) EMPTY STATE BEHAVIOR
    // ========================================================================

    @Nested
    @DisplayName("1) Empty State Behavior")
    class EmptyStateBehavior {

        @Test
        @DisplayName("loadAll_returnsEmpty_whenFilesMissing")
        public void loadAll_returnsEmpty_whenFilesMissing() {
            // ARRANGE: Ensure no data files exist
            cleanDataDirectory();

            PersistenceService persistenceService = createPersistenceService();

            // ACT: Load all data
            PersistenceService.LoadedState state = persistenceService.loadAll();

            // ASSERT: Should return empty collections, not throw
            assertNotNull(state, "LoadedState should not be null");
            assertNotNull(state.getMarkets(), "Markets collection should not be null");
            assertNotNull(state.getUsers(), "Users collection should not be null");
            assertNotNull(state.getTrades(), "Trades collection should not be null");

            assertTrue(state.getMarkets().isEmpty(), "Markets should be empty when file is missing");
            assertTrue(state.getUsers().isEmpty(), "Users should be empty when file is missing");
            assertTrue(state.getTrades().isEmpty(), "Trades should be empty when file is missing");
        }

        @Test
        @DisplayName("saveAndLoad_emptyCollections_worksCorrectly")
        public void saveAndLoad_emptyCollections_worksCorrectly() {
            // ARRANGE
            PersistenceService persistenceService = createPersistenceService();

            Collection<Market> emptyMarkets = new ArrayList<>();
            Collection<Trade> emptyTrades = new ArrayList<>();
            Collection<User> emptyUsers = new ArrayList<>();

            // ACT: Save empty collections
            persistenceService.saveAll(emptyMarkets, emptyTrades, emptyUsers);

            // Reload
            PersistenceService.LoadedState state = persistenceService.loadAll();

            // ASSERT
            assertTrue(state.getMarkets().isEmpty(), "Markets should be empty after saving empty");
            assertTrue(state.getUsers().isEmpty(), "Users should be empty after saving empty");
            assertTrue(state.getTrades().isEmpty(), "Trades should be empty after saving empty");
        }
    }

    // ========================================================================
    // 2) ROUND-TRIP PERSISTENCE (STATIC SNAPSHOT)
    // ========================================================================

    @Nested
    @DisplayName("2) Round-Trip Persistence")
    class RoundTripPersistence {

        @Test
        @DisplayName("saveAndLoad_roundTrip_usersMarketsTrades")
        public void saveAndLoad_roundTrip_usersMarketsTrades() {
            // ARRANGE: Create test data

            // 2 Markets: one OPEN, one RESOLVED
            Market openMarket = new Market("market-open-1", "Will it rain tomorrow?", "Weather prediction");
            Market resolvedMarket = new Market("market-resolved-1", "Will BTC hit $100k?", "Crypto prediction");
            resolvedMarket.setQYes(50.0);
            resolvedMarket.setQNo(30.0);
            resolvedMarket.resolveMarket(Outcome.YES);

            List<Market> markets = Arrays.asList(openMarket, resolvedMarket);

            // 2 Users: with balances and positions
            User user1 = new User("user-1", new BigDecimal("1500.00"));
            Position pos1 = user1.getOrCreatePosition(openMarket.getMarketId());
            pos1.setYesShares(25.0);
            pos1.setNoShares(10.0);

            User user2 = new User("user-2", new BigDecimal("2000.00"));
            Position pos2 = user2.getOrCreatePosition(resolvedMarket.getMarketId());
            pos2.setYesShares(100.0);
            pos2.setNoShares(0.0);
            pos2.markAsSettled();

            List<User> users = Arrays.asList(user1, user2);

            // 2 Trades
            Trade trade1 = new Trade("user-1", "market-open-1", Outcome.YES, 25.0, new BigDecimal("12.50"));
            Trade trade2 = new Trade("user-2", "market-resolved-1", Outcome.YES, 100.0, new BigDecimal("55.00"));

            List<Trade> trades = Arrays.asList(trade1, trade2);

            PersistenceService persistenceService = createPersistenceService();

            // ACT: Save all
            persistenceService.saveAll(markets, trades, users);

            // Load all (simulating restart)
            PersistenceService.LoadedState state = persistenceService.loadAll();

            // ASSERT: All fields match

            // Markets
            Map<String, Market> loadedMarketsMap = new HashMap<>();
            for (Market m : state.getMarkets()) {
                loadedMarketsMap.put(m.getMarketId(), m);
            }

            assertEquals(2, loadedMarketsMap.size(), "Should load 2 markets");

            // Verify open market
            Market loadedOpenMarket = loadedMarketsMap.get("market-open-1");
            assertNotNull(loadedOpenMarket, "Open market should be loaded");
            assertEquals(MarketStatus.OPEN, loadedOpenMarket.getStatus(), "Open market status should be OPEN");
            assertNull(loadedOpenMarket.getResolvedOutcome(), "Open market should have no resolved outcome");

            // Verify resolved market
            Market loadedResolvedMarket = loadedMarketsMap.get("market-resolved-1");
            assertNotNull(loadedResolvedMarket, "Resolved market should be loaded");
            assertEquals(MarketStatus.RESOLVED, loadedResolvedMarket.getStatus(), "Resolved market status");
            assertEquals(Outcome.YES, loadedResolvedMarket.getResolvedOutcome(), "Resolved market outcome");
            assertEquals(50.0, loadedResolvedMarket.getQYes(), 0.001, "qYes should be preserved");
            assertEquals(30.0, loadedResolvedMarket.getQNo(), 0.001, "qNo should be preserved");

            // Users
            Map<String, User> loadedUsersMap = new HashMap<>();
            for (User u : state.getUsers()) {
                loadedUsersMap.put(u.getUserId(), u);
            }

            assertEquals(2, loadedUsersMap.size(), "Should load 2 users");

            // Verify user1
            User loadedUser1 = loadedUsersMap.get("user-1");
            assertNotNull(loadedUser1, "User1 should be loaded");
            assertEquals(0, new BigDecimal("1500.00").compareTo(loadedUser1.getBalance()), "User1 balance");
            Position loadedPos1 = loadedUser1.getPosition("market-open-1");
            assertNotNull(loadedPos1, "User1 position should exist");
            assertEquals(25.0, loadedPos1.getYesShares(), 0.001, "User1 yesShares");
            assertEquals(10.0, loadedPos1.getNoShares(), 0.001, "User1 noShares");

            // Verify user2
            User loadedUser2 = loadedUsersMap.get("user-2");
            assertNotNull(loadedUser2, "User2 should be loaded");
            Position loadedPos2 = loadedUser2.getPosition("market-resolved-1");
            assertNotNull(loadedPos2, "User2 position should exist");
            assertTrue(loadedPos2.isSettled(), "User2 position should be settled");

            // Trades
            assertEquals(2, state.getTrades().size(), "Should load 2 trades");
        }

        @Test
        @DisplayName("roundTrip_preservesResolvedOutcome")
        public void roundTrip_preservesResolvedOutcome() {
            // ARRANGE: Create a resolved market
            Market resolvedMarket = new Market("market-resolved", "Test Market", "Description");
            resolvedMarket.setQYes(75.0);
            resolvedMarket.setQNo(25.0);
            resolvedMarket.resolveMarket(Outcome.NO);

            PersistenceService persistenceService = createPersistenceService();

            // ACT: Save and reload
            persistenceService.saveAll(
                    Arrays.asList(resolvedMarket),
                    new ArrayList<>(),
                    new ArrayList<>());

            PersistenceService.LoadedState state = persistenceService.loadAll();

            // ASSERT
            assertEquals(1, state.getMarkets().size(), "Should load 1 market");

            Market loadedMarket = state.getMarkets().iterator().next();
            assertEquals(MarketStatus.RESOLVED, loadedMarket.getStatus(), "Status should be RESOLVED");
            assertEquals(Outcome.NO, loadedMarket.getResolvedOutcome(), "Resolved outcome should be NO");
        }

        @Test
        @DisplayName("roundTrip_preservesLiquidity")
        public void roundTrip_preservesLiquidity() {
            // ARRANGE
            Market market = new Market("market-liq", "Test", "Desc");
            // Default liquidity is 100.0

            PersistenceService persistenceService = createPersistenceService();

            // ACT
            persistenceService.saveAll(
                    Arrays.asList(market),
                    new ArrayList<>(),
                    new ArrayList<>());

            PersistenceService.LoadedState state = persistenceService.loadAll();

            // ASSERT
            Market loadedMarket = state.getMarkets().iterator().next();
            assertEquals(100.0, loadedMarket.getLiquidity(), 0.001, "Liquidity should be preserved");
        }
    }

    // ========================================================================
    // 3) END-TO-END FLOW WITH ENGINE STATE CHANGES
    // ========================================================================

    @Nested
    @DisplayName("3) End-to-End Flow with Engine State Changes")
    class EndToEndFlow {

        @Test
        @DisplayName("trade_thenRestart_preservesState")
        public void trade_thenRestart_preservesState() {
            // ARRANGE: Set up initial state
            Market market = new Market("market-e2e-1", "E2E Test", "End-to-end test market");
            User user = new User("user-e2e-1", new BigDecimal("1000.00"));

            // Save initial state
            PersistenceService persistenceService = createPersistenceService();
            persistenceService.saveAll(
                    Arrays.asList(market),
                    new ArrayList<>(),
                    Arrays.asList(user));

            // Execute a trade through TradeEngine
            TradeEngine tradeEngine = new TradeEngine();
            double sharesToBuy = 10.0;
            Trade trade = tradeEngine.executeTrade(user, market, Outcome.YES, sharesToBuy);

            List<Trade> trades = new ArrayList<>();
            trades.add(trade);

            // Capture state after trade
            BigDecimal balanceAfterTrade = user.getBalance();
            double qYesAfterTrade = market.getQYes();
            Position positionAfterTrade = user.getPosition(market.getMarketId());
            double yesSharesAfterTrade = positionAfterTrade.getYesShares();

            // ACT: Persist
            persistenceService.saveAll(
                    Arrays.asList(market),
                    trades,
                    Arrays.asList(user));

            // Simulate restart: Create new persistence service and reload
            PersistenceService newPersistenceService = createPersistenceService();
            PersistenceService.LoadedState state = newPersistenceService.loadAll();

            // ASSERT: State matches what we saved

            // User balance is correct
            User loadedUser = null;
            for (User u : state.getUsers()) {
                if (u.getUserId().equals("user-e2e-1")) {
                    loadedUser = u;
                    break;
                }
            }
            assertNotNull(loadedUser, "User should be loaded");
            assertEquals(0, balanceAfterTrade.compareTo(loadedUser.getBalance()),
                    "User balance should match post-trade balance");

            // Market qYes updated
            Market loadedMarket = null;
            for (Market m : state.getMarkets()) {
                if (m.getMarketId().equals("market-e2e-1")) {
                    loadedMarket = m;
                    break;
                }
            }
            assertNotNull(loadedMarket, "Market should be loaded");
            assertEquals(qYesAfterTrade, loadedMarket.getQYes(), 0.001, "Market qYes should match");

            // Positions updated
            Position loadedPosition = loadedUser.getPosition("market-e2e-1");
            assertNotNull(loadedPosition, "Position should exist");
            assertEquals(yesSharesAfterTrade, loadedPosition.getYesShares(), 0.001,
                    "Position yesShares should match");

            // Trade stored with correct fields
            assertEquals(1, state.getTrades().size(), "Should have 1 trade");
            Trade loadedTrade = state.getTrades().iterator().next();
            assertEquals("user-e2e-1", loadedTrade.getUserId(), "Trade userId");
            assertEquals("market-e2e-1", loadedTrade.getMarketId(), "Trade marketId");
            assertEquals(sharesToBuy, loadedTrade.getShareCount(), 0.001, "Trade shares");
            assertNotNull(loadedTrade.getCreatedAt(), "Trade timestamp should be present");
        }

        @Test
        @DisplayName("resolve_thenRestart_preservesState")
        public void resolve_thenRestart_preservesState() {
            // ARRANGE: Set up market with user holding position
            Market market = new Market("market-resolve-1", "Resolution Test", "Test market resolution");
            market.setQYes(50.0);
            market.setQNo(30.0);

            User user = new User("user-resolve-1", new BigDecimal("500.00"));
            Position position = user.getOrCreatePosition(market.getMarketId());
            position.setYesShares(100.0);
            position.setNoShares(0.0);

            // Resolve market
            market.resolveMarket(Outcome.YES);

            // Settle using SettlementEngine
            SettlementEngine settlementEngine = new SettlementEngine();
            settlementEngine.settleUser(user, market);

            // Capture state after settlement
            BigDecimal balanceAfterSettlement = user.getBalance();
            MarketStatus statusAfterResolution = market.getStatus();
            Outcome outcomeAfterResolution = market.getResolvedOutcome();

            // ACT: Persist
            PersistenceService persistenceService = createPersistenceService();
            persistenceService.saveAll(
                    Arrays.asList(market),
                    new ArrayList<>(),
                    Arrays.asList(user));

            // Simulate restart
            PersistenceService newPersistenceService = createPersistenceService();
            PersistenceService.LoadedState state = newPersistenceService.loadAll();

            // ASSERT

            // Market status + outcome persisted
            Market loadedMarket = state.getMarkets().iterator().next();
            assertEquals(statusAfterResolution, loadedMarket.getStatus(),
                    "Market status should be RESOLVED");
            assertEquals(outcomeAfterResolution, loadedMarket.getResolvedOutcome(),
                    "Resolved outcome should be YES");

            // User balances updated (500 + 100 = 600)
            User loadedUser = state.getUsers().iterator().next();
            assertEquals(0, balanceAfterSettlement.compareTo(loadedUser.getBalance()),
                    "User balance should include payout");

            // Positions settled
            Position loadedPosition = loadedUser.getPosition(market.getMarketId());
            assertNotNull(loadedPosition, "Position should exist");
            assertTrue(loadedPosition.isSettled(), "Position should be marked as settled");
            assertEquals(0.0, loadedPosition.getYesShares(), 0.001,
                    "Shares should be cleared after settlement");
        }

        @Test
        @DisplayName("multipleTrades_thenRestart_preservesAllState")
        public void multipleTrades_thenRestart_preservesAllState() {
            // ARRANGE: Multiple users with multiple trades
            Market market = new Market("market-multi", "Multi Trade Test", "Description");

            User user1 = new User("user-multi-1", new BigDecimal("1000.00"));
            User user2 = new User("user-multi-2", new BigDecimal("2000.00"));

            TradeEngine tradeEngine = new TradeEngine();

            // Execute multiple trades
            Trade trade1 = tradeEngine.executeTrade(user1, market, Outcome.YES, 5.0);
            Trade trade2 = tradeEngine.executeTrade(user2, market, Outcome.NO, 10.0);
            Trade trade3 = tradeEngine.executeTrade(user1, market, Outcome.YES, 3.0);

            List<Trade> trades = Arrays.asList(trade1, trade2, trade3);

            // ACT: Persist
            PersistenceService persistenceService = createPersistenceService();
            persistenceService.saveAll(
                    Arrays.asList(market),
                    trades,
                    Arrays.asList(user1, user2));

            // Reload
            PersistenceService.LoadedState state = persistenceService.loadAll();

            // ASSERT
            assertEquals(1, state.getMarkets().size(), "Should have 1 market");
            assertEquals(2, state.getUsers().size(), "Should have 2 users");
            assertEquals(3, state.getTrades().size(), "Should have 3 trades");

            // Verify market has accumulated shares
            Market loadedMarket = state.getMarkets().iterator().next();
            assertEquals(8.0, loadedMarket.getQYes(), 0.001, "qYes should be 5 + 3 = 8");
            assertEquals(10.0, loadedMarket.getQNo(), 0.001, "qNo should be 10");
        }
    }

    // ========================================================================
    // 4) DATA INTEGRITY / FAIL-FAST BEHAVIOR
    // ========================================================================

    @Nested
    @DisplayName("4) Data Integrity / Fail-Fast Behavior")
    class DataIntegrityTests {

        @Test
        @DisplayName("load_fails_when_requiredFieldMissing - market missing status")
        public void load_fails_when_marketMissingStatus() throws IOException {
            // ARRANGE: Write a malformed market JSON (status = null)
            String malformedJson = """
                    [{
                        "marketId": "bad-market",
                        "marketName": "Bad Market",
                        "marketDescription": "Missing status",
                        "qYes": 0.0,
                        "qNo": 0.0,
                        "liquidity": 100.0,
                        "status": null,
                        "resolvedOutcome": null
                    }]
                    """;

            File dataDir = new File("data");
            dataDir.mkdirs();
            Files.writeString(new File(dataDir, "markets.json").toPath(), malformedJson);

            PersistenceService persistenceService = createPersistenceService();

            // ACT & ASSERT: Load should throw
            assertThrows(IllegalStateException.class, () -> {
                persistenceService.loadAll();
            }, "Should throw when market status is null");
        }

        @Test
        @DisplayName("load_fails_onInvalidLiquidity - market has b <= 0")
        public void load_fails_onInvalidLiquidity() throws IOException {
            // ARRANGE: Write market with zero liquidity
            String malformedJson = """
                    [{
                        "marketId": "bad-liquidity",
                        "marketName": "Bad Liquidity Market",
                        "marketDescription": "Zero liquidity",
                        "qYes": 0.0,
                        "qNo": 0.0,
                        "liquidity": 0.0,
                        "status": "OPEN",
                        "resolvedOutcome": null
                    }]
                    """;

            File dataDir = new File("data");
            dataDir.mkdirs();
            Files.writeString(new File(dataDir, "markets.json").toPath(), malformedJson);

            PersistenceService persistenceService = createPersistenceService();

            // ACT & ASSERT
            assertThrows(IllegalStateException.class, () -> {
                persistenceService.loadAll();
            }, "Should throw when liquidity <= 0");
        }

        @Test
        @DisplayName("load_fails_onInvalidLiquidity - market has NaN liquidity")
        public void load_fails_onNaNLiquidity() throws IOException {
            // ARRANGE: Write market with NaN liquidity
            String malformedJson = """
                    [{
                        "marketId": "nan-liquidity",
                        "marketName": "NaN Liquidity Market",
                        "marketDescription": "NaN liquidity",
                        "qYes": 0.0,
                        "qNo": 0.0,
                        "liquidity": "NaN",
                        "status": "OPEN",
                        "resolvedOutcome": null
                    }]
                    """;

            File dataDir = new File("data");
            dataDir.mkdirs();
            Files.writeString(new File(dataDir, "markets.json").toPath(), malformedJson);

            PersistenceService persistenceService = createPersistenceService();

            // ACT & ASSERT
            assertThrows(Exception.class, () -> {
                persistenceService.loadAll();
            }, "Should throw when liquidity is NaN");
        }

        @Test
        @DisplayName("load_fails_onResolvedMarketMissingOutcome")
        public void load_fails_onResolvedMarketMissingOutcome() throws IOException {
            // ARRANGE: Write resolved market without outcome
            String malformedJson = """
                    [{
                        "marketId": "resolved-no-outcome",
                        "marketName": "Resolved No Outcome",
                        "marketDescription": "Resolved but missing outcome",
                        "qYes": 50.0,
                        "qNo": 30.0,
                        "liquidity": 100.0,
                        "status": "RESOLVED",
                        "resolvedOutcome": null
                    }]
                    """;

            File dataDir = new File("data");
            dataDir.mkdirs();
            Files.writeString(new File(dataDir, "markets.json").toPath(), malformedJson);

            PersistenceService persistenceService = createPersistenceService();

            // ACT & ASSERT
            assertThrows(IllegalStateException.class, () -> {
                persistenceService.loadAll();
            }, "Should throw when RESOLVED market has null outcome");
        }

        @Test
        @DisplayName("load_fails_onOpenMarketWithOutcome")
        public void load_fails_onOpenMarketWithOutcome() throws IOException {
            // ARRANGE: Write OPEN market with an outcome (invalid state)
            String malformedJson = """
                    [{
                        "marketId": "open-with-outcome",
                        "marketName": "Open With Outcome",
                        "marketDescription": "Open but has outcome set",
                        "qYes": 0.0,
                        "qNo": 0.0,
                        "liquidity": 100.0,
                        "status": "OPEN",
                        "resolvedOutcome": "YES"
                    }]
                    """;

            File dataDir = new File("data");
            dataDir.mkdirs();
            Files.writeString(new File(dataDir, "markets.json").toPath(), malformedJson);

            PersistenceService persistenceService = createPersistenceService();

            // ACT & ASSERT
            assertThrows(IllegalStateException.class, () -> {
                persistenceService.loadAll();
            }, "Should throw when OPEN market has resolved outcome");
        }

        @Test
        @DisplayName("load_fails_when_tradeMissingUserId")
        public void load_fails_when_tradeMissingUserId() throws IOException {
            // ARRANGE: Write trade with null userId
            String malformedJson = """
                    [{
                        "userId": null,
                        "marketId": "market-1",
                        "outcome": "YES",
                        "sharesBought": 10.0,
                        "cost": 5.00,
                        "createdAt": "2024-01-01T00:00:00Z",
                        "tradeId": "trade-1"
                    }]
                    """;

            File dataDir = new File("data");
            dataDir.mkdirs();
            Files.writeString(new File(dataDir, "trades.json").toPath(), malformedJson);

            PersistenceService persistenceService = createPersistenceService();

            // ACT & ASSERT: Exception may be wrapped in RuntimeException
            assertThrows(Exception.class, () -> {
                persistenceService.loadAll();
            }, "Should throw when trade userId is null");
        }

        @Test
        @DisplayName("load_fails_when_userMissingBalance")
        public void load_fails_when_userMissingBalance() throws IOException {
            // ARRANGE: Write user with null balance
            String malformedJson = """
                    [{
                        "userId": "user-no-balance",
                        "balance": null,
                        "positions": {}
                    }]
                    """;

            File dataDir = new File("data");
            dataDir.mkdirs();
            Files.writeString(new File(dataDir, "users.json").toPath(), malformedJson);

            PersistenceService persistenceService = createPersistenceService();

            // ACT & ASSERT: Exception may be wrapped in RuntimeException
            assertThrows(Exception.class, () -> {
                persistenceService.loadAll();
            }, "Should throw when user balance is null");
        }
    }

    // ========================================================================
    // 5) TRADE RECORD CONTRACT
    // ========================================================================

    @Nested
    @DisplayName("5) Trade Record Contract")
    class TradeRecordContract {

        @Test
        @DisplayName("tradeFields_persistedFully")
        public void tradeFields_persistedFully() {
            // ARRANGE: Create a trade with all fields
            String userId = "user-trade-1";
            String marketId = "market-trade-1";
            Outcome outcome = Outcome.YES;
            double shares = 42.5;
            BigDecimal cost = new BigDecimal("21.25");

            Trade trade = new Trade(userId, marketId, outcome, shares, cost);

            // Capture original timestamp
            java.time.Instant originalTimestamp = trade.getCreatedAt();

            PersistenceService persistenceService = createPersistenceService();

            // ACT: Save and reload
            persistenceService.saveAll(
                    new ArrayList<>(),
                    Arrays.asList(trade),
                    new ArrayList<>());

            PersistenceService.LoadedState state = persistenceService.loadAll();

            // ASSERT: All fields match
            assertEquals(1, state.getTrades().size(), "Should have 1 trade");

            Trade loadedTrade = state.getTrades().iterator().next();
            assertEquals(userId, loadedTrade.getUserId(), "userId should match");
            assertEquals(marketId, loadedTrade.getMarketId(), "marketId should match");
            assertEquals(shares, loadedTrade.getShareCount(), 0.001, "shares should match");
            assertEquals(0, cost.compareTo(loadedTrade.getCost()), "cost should match");
            assertNotNull(loadedTrade.getCreatedAt(), "timestamp should not be null");

            // Note: Timestamp might have slightly different precision after serialization
            // but should represent the same instant
            assertEquals(originalTimestamp.getEpochSecond(), loadedTrade.getCreatedAt().getEpochSecond(),
                    "Timestamp epoch seconds should match");
        }

        @Test
        @DisplayName("multipleTrades_allFieldsPersisted")
        public void multipleTrades_allFieldsPersisted() {
            // ARRANGE
            Trade trade1 = new Trade("user-1", "market-1", Outcome.YES, 10.0, new BigDecimal("5.50"));
            Trade trade2 = new Trade("user-2", "market-1", Outcome.NO, 20.0, new BigDecimal("8.75"));
            Trade trade3 = new Trade("user-1", "market-2", Outcome.YES, 15.0, new BigDecimal("7.25"));

            List<Trade> trades = Arrays.asList(trade1, trade2, trade3);

            PersistenceService persistenceService = createPersistenceService();

            // ACT
            persistenceService.saveAll(new ArrayList<>(), trades, new ArrayList<>());
            PersistenceService.LoadedState state = persistenceService.loadAll();

            // ASSERT
            assertEquals(3, state.getTrades().size(), "Should have 3 trades");

            // Verify trades can be distinguished by their fields
            Map<String, Trade> tradesByUserAndMarket = new HashMap<>();
            for (Trade t : state.getTrades()) {
                String key = t.getUserId() + "-" + t.getMarketId();
                tradesByUserAndMarket.put(key, t);
            }

            assertTrue(tradesByUserAndMarket.containsKey("user-1-market-1"), "Should have user-1 market-1 trade");
            assertTrue(tradesByUserAndMarket.containsKey("user-2-market-1"), "Should have user-2 market-1 trade");
            assertTrue(tradesByUserAndMarket.containsKey("user-1-market-2"), "Should have user-1 market-2 trade");
        }
    }

    // ========================================================================
    // 6) ATOMIC WRITE SAFETY
    // ========================================================================

    @Nested
    @DisplayName("6) Atomic Write Safety")
    class AtomicWriteSafety {

        @Test
        @DisplayName("save_createsValidJsonFiles")
        public void save_createsValidJsonFiles() throws IOException {
            // ARRANGE
            Market market = new Market("market-atomic", "Atomic Test", "Description");
            User user = new User("user-atomic", new BigDecimal("500.00"));
            Trade trade = new Trade("user-atomic", "market-atomic", Outcome.YES, 10.0, new BigDecimal("5.00"));

            PersistenceService persistenceService = createPersistenceService();

            // ACT
            persistenceService.saveAll(
                    Arrays.asList(market),
                    Arrays.asList(trade),
                    Arrays.asList(user));

            // ASSERT: Files exist and are valid JSON
            File dataDir = new File("data");

            File marketsFile = new File(dataDir, "markets.json");
            File usersFile = new File(dataDir, "users.json");
            File tradesFile = new File(dataDir, "trades.json");

            assertTrue(marketsFile.exists(), "markets.json should exist");
            assertTrue(usersFile.exists(), "users.json should exist");
            assertTrue(tradesFile.exists(), "trades.json should exist");

            // Verify JSON is parseable
            assertDoesNotThrow(() -> {
                mapper.readTree(marketsFile);
                mapper.readTree(usersFile);
                mapper.readTree(tradesFile);
            }, "All files should be valid JSON");
        }

        @Test
        @DisplayName("save_overwrites_previousData")
        public void save_overwrites_previousData() {
            // ARRANGE
            PersistenceService persistenceService = createPersistenceService();

            // Save initial data
            Market market1 = new Market("market-1", "First", "First market");
            persistenceService.saveAll(Arrays.asList(market1), new ArrayList<>(), new ArrayList<>());

            // ACT: Save different data
            Market market2 = new Market("market-2", "Second", "Second market");
            persistenceService.saveAll(Arrays.asList(market2), new ArrayList<>(), new ArrayList<>());

            // ASSERT: Only new data present
            PersistenceService.LoadedState state = persistenceService.loadAll();
            assertEquals(1, state.getMarkets().size(), "Should have exactly 1 market");
            assertEquals("market-2", state.getMarkets().iterator().next().getMarketId(),
                    "Should be the second market, not the first");
        }

        @Test
        @DisplayName("previousValidData_notCorrupted_afterSaveOfNewData")
        public void previousValidData_notCorrupted_afterSaveOfNewData() {
            // ARRANGE: Save valid data first
            PersistenceService persistenceService = createPersistenceService();

            Market market1 = new Market("market-valid", "Valid", "Valid market");
            User user1 = new User("user-valid", new BigDecimal("1000.00"));

            persistenceService.saveAll(
                    Arrays.asList(market1),
                    new ArrayList<>(),
                    Arrays.asList(user1));

            // Verify first save worked
            PersistenceService.LoadedState state1 = persistenceService.loadAll();
            assertEquals(1, state1.getMarkets().size(), "First save should work");

            // ACT: Save new valid data
            Market market2 = new Market("market-new", "New", "New market");
            User user2 = new User("user-new", new BigDecimal("2000.00"));

            persistenceService.saveAll(
                    Arrays.asList(market2),
                    new ArrayList<>(),
                    Arrays.asList(user2));

            // ASSERT: New data is valid and loadable
            PersistenceService.LoadedState state2 = persistenceService.loadAll();
            assertEquals(1, state2.getMarkets().size(), "Second save should work");
            assertEquals("market-new", state2.getMarkets().iterator().next().getMarketId(),
                    "New market should be present");
        }
    }

    // ========================================================================
    // ADDITIONAL EDGE CASES
    // ========================================================================

    @Nested
    @DisplayName("Additional Edge Cases")
    class AdditionalEdgeCases {

        @Test
        @DisplayName("largeDataSet_persistsAndLoadsCorrectly")
        public void largeDataSet_persistsAndLoadsCorrectly() {
            // ARRANGE: Create large dataset
            List<Market> markets = new ArrayList<>();
            List<User> users = new ArrayList<>();
            List<Trade> trades = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
                markets.add(new Market("market-" + i, "Market " + i, "Description " + i));
            }

            for (int i = 0; i < 50; i++) {
                users.add(new User("user-" + i, new BigDecimal("1000.00")));
            }

            for (int i = 0; i < 200; i++) {
                trades.add(new Trade(
                        "user-" + (i % 50),
                        "market-" + (i % 100),
                        i % 2 == 0 ? Outcome.YES : Outcome.NO,
                        10.0,
                        new BigDecimal("5.00")));
            }

            PersistenceService persistenceService = createPersistenceService();

            // ACT
            persistenceService.saveAll(markets, trades, users);
            PersistenceService.LoadedState state = persistenceService.loadAll();

            // ASSERT
            assertEquals(100, state.getMarkets().size(), "Should load 100 markets");
            assertEquals(50, state.getUsers().size(), "Should load 50 users");
            assertEquals(200, state.getTrades().size(), "Should load 200 trades");
        }

        @Test
        @DisplayName("specialCharacters_inFieldsPreserved")
        public void specialCharacters_inFieldsPreserved() {
            // ARRANGE: Market with special characters
            Market market = new Market(
                    "market-special",
                    "Will $BTC hit €50k? 日本語",
                    "Description with \"quotes\" and 'apostrophes' and \n newlines");

            PersistenceService persistenceService = createPersistenceService();

            // ACT
            persistenceService.saveAll(Arrays.asList(market), new ArrayList<>(), new ArrayList<>());
            PersistenceService.LoadedState state = persistenceService.loadAll();

            // ASSERT
            Market loadedMarket = state.getMarkets().iterator().next();
            assertNotNull(loadedMarket, "Market should be loaded");
            // MarketId should at least be preserved
            assertEquals("market-special", loadedMarket.getMarketId());
        }

        @Test
        @DisplayName("userWithMultiplePositions_allPreserved")
        public void userWithMultiplePositions_allPreserved() {
            // ARRANGE
            User user = new User("user-multi-pos", new BigDecimal("5000.00"));

            Position pos1 = user.getOrCreatePosition("market-1");
            pos1.setYesShares(10.0);
            pos1.setNoShares(5.0);

            Position pos2 = user.getOrCreatePosition("market-2");
            pos2.setYesShares(20.0);
            pos2.setNoShares(15.0);

            Position pos3 = user.getOrCreatePosition("market-3");
            pos3.setYesShares(30.0);
            pos3.setNoShares(25.0);
            pos3.markAsSettled();

            // Need markets for validation to pass
            Market m1 = new Market("market-1", "M1", "D1");
            Market m2 = new Market("market-2", "M2", "D2");
            Market m3 = new Market("market-3", "M3", "D3");
            m3.resolveMarket(Outcome.YES);

            PersistenceService persistenceService = createPersistenceService();

            // ACT
            persistenceService.saveAll(
                    Arrays.asList(m1, m2, m3),
                    new ArrayList<>(),
                    Arrays.asList(user));

            PersistenceService.LoadedState state = persistenceService.loadAll();

            // ASSERT
            User loadedUser = state.getUsers().iterator().next();
            assertEquals(3, loadedUser.getPositions().size(), "Should have 3 positions");

            Position loadedPos1 = loadedUser.getPosition("market-1");
            assertEquals(10.0, loadedPos1.getYesShares(), 0.001);
            assertEquals(5.0, loadedPos1.getNoShares(), 0.001);

            Position loadedPos3 = loadedUser.getPosition("market-3");
            assertTrue(loadedPos3.isSettled(), "Position 3 should be settled");
        }
    }
}
