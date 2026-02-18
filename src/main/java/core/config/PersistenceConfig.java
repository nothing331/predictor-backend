package core.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import core.repository.port.MarketRepository;
import core.repository.port.TradeRepository;
import core.repository.port.UserRepository;

@Configuration
public class PersistenceConfig {

    // Market Repository Configuration

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.persistence.mode", havingValue = "json")
    public MarketRepository jsonMarketRepository(@Qualifier("marketJsonAdapter") MarketRepository repository) {
        return repository;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.persistence.mode", havingValue = "db")
    public MarketRepository dbMarketRepository(@Qualifier("marketDbAdapter") MarketRepository repository) {
        return repository;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.persistence.mode", havingValue = "dual", matchIfMissing = true)
    public MarketRepository dualMarketRepository(@Qualifier("marketDualAdapter") MarketRepository repository) {
        return repository;
    }

    // User Repository Configuration

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.persistence.mode", havingValue = "json")
    public UserRepository jsonUserRepository(@Qualifier("userJsonAdapter") UserRepository repository) {
        return repository;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.persistence.mode", havingValue = "db")
    public UserRepository dbUserRepository(@Qualifier("userDbAdapter") UserRepository repository) {
        return repository;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.persistence.mode", havingValue = "dual", matchIfMissing = true)
    public UserRepository dualUserRepository(@Qualifier("userDualAdapter") UserRepository repository) {
        return repository;
    }

    // Trade Repository Configuration

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.persistence.mode", havingValue = "json")
    public TradeRepository jsonTradeRepository(@Qualifier("tradeJsonAdapter") TradeRepository repository) {
        return repository;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.persistence.mode", havingValue = "db")
    public TradeRepository dbTradeRepository(@Qualifier("tradeDbAdapter") TradeRepository repository) {
        return repository;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.persistence.mode", havingValue = "dual", matchIfMissing = true)
    public TradeRepository dualTradeRepository(@Qualifier("tradeDualAdapter") TradeRepository repository) {
        return repository;
    }
}
