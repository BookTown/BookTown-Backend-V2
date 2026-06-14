package com.booktown.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final MongoTemplate mongoTemplate;

    @Value("${spring.ai.vectorstore.chroma.client.host:http://localhost}")
    private String chromaHost;

    @Value("${spring.ai.vectorstore.chroma.client.port:8000}")
    private int chromaPort;

    private static final RestClient CHROMA_CLIENT;

    static {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(3));
        CHROMA_CLIENT = RestClient.builder().requestFactory(factory).build();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", Map.of("status", "UP"));
        body.put("meta", null);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/health/readiness")
    public ResponseEntity<Map<String, Object>> checkReadiness() {
        boolean healthy = checkMysql() && checkRedis() && checkMongo() && checkChroma();

        if (healthy) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("data", Map.of("status", "UP"));
            body.put("meta", null);
            return ResponseEntity.ok(body);
        }

        Map<String, Object> error = Map.of(
                "code", "SERVICE_UNAVAILABLE",
                "message", "하나 이상의 의존 서비스가 준비되지 않았습니다.",
                "fieldErrors", List.of()
        );
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("traceId", null);
        return ResponseEntity.status(503).body(body);
    }

    private boolean checkMysql() {
        try {
            jdbcTemplate.execute("SELECT 1");
            return true;
        } catch (Exception e) {
            log.warn("MySQL readiness check failed");
            return false;
        }
    }

    private boolean checkRedis() {
        RedisConnection connection = null;
        try {
            connection = redisTemplate.getConnectionFactory().getConnection();
            connection.ping();
            return true;
        } catch (Exception e) {
            log.warn("Redis readiness check failed");
            return false;
        } finally {
            if (connection != null) {
                RedisConnectionUtils.releaseConnection(connection, redisTemplate.getConnectionFactory());
            }
        }
    }

    private boolean checkMongo() {
        try {
            mongoTemplate.getDb().runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            log.warn("MongoDB readiness check failed");
            return false;
        }
    }

    private boolean checkChroma() {
        try {
            CHROMA_CLIENT.get()
                    .uri(chromaHost + ":" + chromaPort + "/api/v2/heartbeat")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("ChromaDB readiness check failed");
            return false;
        }
    }
}
