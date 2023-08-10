package com.allback.cygiconcert.config.interceptor;


import com.google.common.net.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.BasicJsonParser;
import org.springframework.boot.json.JsonParser;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Base64;
import java.util.Map;

@Component
public class QueueInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(QueueInterceptor.class);
    private static final Base64.Decoder decoder = Base64.getUrlDecoder();
    private static final JsonParser jsonParser = new BasicJsonParser();
    private static final String KEY = "queue";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        // 3초 대기 (일부러 성능 떨어뜨리기)
        Thread.sleep(3000);

        String jwt = request.getHeader(HttpHeaders.AUTHORIZATION).replace("Bearer", "");
        String[] chunks = jwt.split("\\.");
        String payload = new String(decoder.decode(chunks[1]));
        Map<String, Object> jsonArray = jsonParser.parseMap(payload);

        String COMMAND = request.getHeader("QUEUE");
        String value = jsonArray.get("userId").toString(); // JWT에서 뽑아낸 사용자 아이디

        // redis 대기열에 저장된 user id 데이터 삭제하기
        redisTemplate.opsForZSet().remove(KEY, value);
        logger.info("redis data removed after processing");

    }
}
