package com.space.munova.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Objects;

@Slf4j
@Configuration
public class TomcatDebugConfig {

    private final Environment env;

    public TomcatDebugConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public TomcatServletWebServerFactory tomcatFactory(Environment env) {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();

        factory.addConnectorCustomizers(connector -> {
            Http11NioProtocol p = (Http11NioProtocol) connector.getProtocolHandler();

            Integer maxConn = env.getProperty("server.tomcat.max-connections", Integer.class, 8192);
            Integer acceptCount = env.getProperty("server.tomcat.accept-count", Integer.class, 100);
            Integer maxThreads = env.getProperty("server.tomcat.threads.max", Integer.class, 200);
            Integer minSpare = env.getProperty("server.tomcat.threads.min-spare", Integer.class, 10);

            p.setMaxConnections(maxConn);
            p.setAcceptCount(acceptCount);
            p.setMaxThreads(maxThreads);
            p.setMinSpareThreads(minSpare);

            log.info("========= TOMCAT RUNTIME CONFIG =========");
            log.info("maxConnections: {}", p.getMaxConnections());
            log.info("acceptCount:    {}", p.getAcceptCount());
            log.info("maxThreads:     {}", p.getMaxThreads());
            log.info("minSpareThreads:{}", p.getMinSpareThreads());
            log.info("connectionTimeout: {}", p.getConnectionTimeout());
            log.info("========================================");
        });

        return factory;
    }
}
