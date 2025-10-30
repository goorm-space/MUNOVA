    package com.space.munova.core.config;

    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
    import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

    @Configuration
    public class WebConfig implements WebMvcConfigurer {

        // .properties 파일에서 값을 읽어옴
        @Value("${file.dir}")
        private String storagePath;

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/static/munovaImg/**") // 1. 클라이언트가 요청할 URL 패턴 (규칙과 일치)
                    .addResourceLocations("file:" + storagePath);         // 2. 서버의 실제 파일이 위치한 경로
        }
    }
