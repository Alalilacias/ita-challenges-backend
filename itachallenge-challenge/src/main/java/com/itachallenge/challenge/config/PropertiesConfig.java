package com.itachallenge.challenge.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component  //@Configuration not used due there isn't any @bean method
public class PropertiesConfig {
    private static final Logger log = LoggerFactory.getLogger(PropertiesConfig.class);

    @Value("${url.connection_timeout}")
    private Integer connectionTimeout;//millis
    @Value("${url.maxBytesInMemory}")
    private Integer maxBytesInMemory;
    @Value("${levels.easy.description}")
    private String easy;
    @Value("${levels.medium.description}")
    private String medium;
    @Value("${levels.hard.description}")
    private String hard;
    @Value("${languages.java.description}")
    private String java;
    @Value("${languages.javascript.description}")
    private String javascript;
    @Value("${languages.php.description}")
    private String php;
    @Value("${languages.python.description}")
    private String python;

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }
    public Integer getMaxBytesInMemory() {
        return maxBytesInMemory;
    }
    public String getEasy() { return easy; }
    public String getMedium() { return medium; }
    public String getHard() { return hard; }
    public String getJava() { return java; }
    public String getJavascript() { return javascript; }
    public String getPhp() { return php; }
    public String getPython() { return python; }

}
