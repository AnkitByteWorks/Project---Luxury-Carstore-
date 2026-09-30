package com.Luxurycars.carstore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@Profile("!test")
@EnableMongoRepositories(basePackages = "com.Luxurycars.carstore.mongodb.repository")
public class MongoConfig {
}
