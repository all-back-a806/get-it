package com.allback.gateway.filter;

import com.allback.gateway.config.RedisConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class RedisRequestFilter extends AbstractGatewayFilterFactory<RedisRequestFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(RedisRequestFilter.class);
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    private ZSetOperations<String, String> zSetOps;
    private final String KEY = "queue";

    /**
     * Client에게 보낼 대기표 Response 형식
     */
    @Data
    @AllArgsConstructor
    static class JsonResponse {
        private Long rank;
        private Long size;
    }

    public static class Config {}

    public RedisRequestFilter() {
        super(Config.class);
//        zSetOps = redisTemplate.opsForZSet();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            ServerHttpRequest request = exchange.getRequest();

            // 대기열을 거치지 않는 요청 (header에 QUEUE가 없다)
            if (!request.getHeaders().containsKey("QUEUE")) {
                return chain.filter(exchange);
            }

            String COMMAND = request.getHeaders().get("QUEUE").get(0);
            String value = "user3"; // TODO : JWT에서 사용자 아이디를 뽑아내서 value 값으로 사용하기
            double score = 0d;  // 정렬 기준(대기표 발급 시각). 작을수록 순위가 높다.


            // 1. 최초 요청
            if (COMMAND.equals("FIRST")) {
                // redis에 넣기
                score = System.currentTimeMillis();
                redisTemplate.opsForZSet().add(KEY, value, score);
            }

            // 2. 재요청
            else if (COMMAND.equals("RE")) {
                // 특별히 필요한 코드는 없다.
            }

            // 3. 대기 취소 요청
            else if (COMMAND.equals("QUIT")) {
                // redis에서 해당 데이터 삭제
                redisTemplate.opsForZSet().remove(KEY, value);

                // 응답 만들기
                ServerHttpResponse response = exchange.getResponse();

                // 200 상태 코드로 응답 설정
                response.setStatusCode(HttpStatus.OK);

                // 응답 본문에 메시지 설정
                String message = "waiting is successfully canceled";
                DataBuffer buffer = response.bufferFactory().wrap(message.getBytes());
                return response.writeWith(Mono.just(buffer))
                        .flatMap(Void -> Mono.error(new ResponseStatusException(HttpStatus.OK, message)));
            }


            Long rank = redisTemplate.opsForZSet().rank(KEY, value);    // 내가 몇 등인지
            Long size = redisTemplate.opsForZSet().size(KEY);   // 총 몇 명이 대기 중인지

            // 1. 내 차례일 경우
            if (rank == 0) {
                return chain.filter(exchange);
            }

            // 2. 대기해야하는 경우
            else {
                ServerHttpResponse response = exchange.getResponse();

                response.setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
                response.getHeaders().add("Content-Type", "application/json");

                JsonResponse jsonResponse = new JsonResponse(rank, size);

                // JSON 문자열로 변환
                ObjectMapper objectMapper = new ObjectMapper();

                String responseBody;
                try {
                    responseBody = objectMapper.writeValueAsString(jsonResponse);
                } catch (JsonProcessingException e) {
                    // JSON 변환 실패 시 에러 응답 전송
                    response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    responseBody = "Error: " + e.getMessage();
                }

                byte[] responseBytes = responseBody.getBytes();

                // 응답을 클라이언트에게 전송하고 filter 체인 종료
                return response.writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(responseBytes)));
            }
        };
    }
}
