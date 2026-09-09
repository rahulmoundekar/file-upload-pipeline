package com.rahul.security;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="security.api")
public record SecurityProperties(boolean enabled,String token){ }
