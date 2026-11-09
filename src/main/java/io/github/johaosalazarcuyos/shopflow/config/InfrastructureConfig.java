package io.github.johaosalazarcuyos.shopflow.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
public class InfrastructureConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
