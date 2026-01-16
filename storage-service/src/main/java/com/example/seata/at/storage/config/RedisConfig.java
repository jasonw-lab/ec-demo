package com.example.seata.at.storage.config;

import com.example.seata.at.storage.api.dto.ProductResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.password:}") String password,
            @Value("${spring.data.redis.ssl.enabled:false}") boolean sslEnabled) {

        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        if (password != null && !password.isBlank()) {
            configuration.setPassword(RedisPassword.of(password));
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
        if (sslEnabled) {
            builder.useSsl();
        }
        LettuceClientConfiguration clientConfig = builder.build();

        return new LettuceConnectionFactory(configuration, clientConfig);
    }

    @Bean
    public RedisTemplate<String, ProductResponse> productRedisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, ProductResponse> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(ProductResponse.class));
        template.afterPropertiesSet();
        return template;
    }
}
