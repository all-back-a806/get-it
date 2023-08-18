package com.allback.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ScheduledTask {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Scheduled(fixedRate = 5000) // 5초마다 실행
    public void printHello() {
        Set<String> queue = redisTemplate.opsForZSet().range("queue", 0, 10);
        for (String userId : queue) {
            // 만료시간이 지났다면
            if (!redisTemplate.hasKey(userId)) {
                redisTemplate.opsForZSet().remove("queue", userId);
                System.out.println("removed!!!!!");
            }
        }
    }
}
