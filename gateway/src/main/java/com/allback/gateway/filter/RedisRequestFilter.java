package com.allback.gateway.filter;

import com.allback.gateway.config.RedisConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.BasicJsonParser;
import org.springframework.boot.json.JsonParser;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.*;

@Component
public class RedisRequestFilter extends AbstractGatewayFilterFactory<RedisRequestFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(RedisRequestFilter.class);
    private static final Base64.Decoder decoder = Base64.getUrlDecoder();
    private static final JsonParser jsonParser = new BasicJsonParser();
    private static final String KEY = "queue";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;


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
            Optional<List<String>> comm = Optional.ofNullable(request.getHeaders().get("QUEUE"));

            // 대기열을 거치지 않는 요청 (header에 QUEUE가 없다)
            if (comm.isPresent() && comm.get().get(0).equals("PASS")) {
                return chain.filter(exchange);
            }

            String jwt = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0).replace("Bearer", "");
            String[] chunks = jwt.split("\\.");
            String payload = new String(decoder.decode(chunks[1]));
            Map<String, Object> jsonArray = jsonParser.parseMap(payload);


            String value = jsonArray.get("userId").toString(); // JWT에서 뽑아낸 사용자 아이디
            Double score = redisTemplate.opsForZSet().score(KEY, value);  // 정렬 기준(대기표 발급 시각). 작을수록 순위가 높다.

            Long rank = redisTemplate.opsForZSet().rank(KEY, value);    // 내가 몇 등인지
            Long size = redisTemplate.opsForZSet().size(KEY);   // 총 몇 명이 대기 중인지

            // 1. 최초 요청
            if (rank == null) {
                // redis에 넣기
                score = (double)System.currentTimeMillis();
                redisTemplate.opsForZSet().addIfAbsent(KEY, value, score);
                rank = redisTemplate.opsForZSet().rank(KEY, value);    // 내가 몇 등인지
                size = redisTemplate.opsForZSet().size(KEY);
            }

            // 3. 대기 취소 요청
            else if (comm.isPresent() && comm.get().get(0).equals("QUIT")) {
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

            logger.info("rank : " + rank + ", size : " + size);

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
