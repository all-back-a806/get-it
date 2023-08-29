package com.allback.cygiconcert.config;

//import com.allback.cygiconcert.config.interceptor.KafkaInterceptor;
import com.allback.cygiconcert.config.interceptor.QueueInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private QueueInterceptor queueInterceptor;
//    private KafkaInterceptor kafkaInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("*")
            .allowCredentials(true) // 자격증명 허용
            .allowedHeaders("*")
            .exposedHeaders("*")
            .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(queueInterceptor);
    }
}
