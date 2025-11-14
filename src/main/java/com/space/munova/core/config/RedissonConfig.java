package com.space.munova.core.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private String port;
    @Value("${spring.data.redis.password}")
    private String password;
    @Value("${spring.data.redis.min-idle}")
    private int minIdle;
    @Value("${spring.data.redis.pool-size}")
    private int poolSize;
    @Value("${spring.data.redis.timeout}")
    private int timeout;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setPassword(password)
                .setConnectionMinimumIdleSize(minIdle)
                .setConnectionPoolSize(poolSize)
                .setTimeout(timeout);

        return Redisson.create(config);
    }
}
