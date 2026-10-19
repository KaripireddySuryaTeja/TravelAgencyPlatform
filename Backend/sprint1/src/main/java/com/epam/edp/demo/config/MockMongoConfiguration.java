package com.epam.edp.demo.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.h2.H2Backend;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("mock-mongo")
public class MockMongoConfiguration {

    @Bean(destroyMethod = "shutdown")
    public MongoServer mockMongoServer() {
        MongoServer server = new MongoServer(H2Backend.inMemory());
        server.bind("127.0.0.1", 27017);
        return server;
    }

    @Bean(destroyMethod = "close")
    @Primary
    @DependsOn("mockMongoServer")
    public MongoClient mongoClient(MongoServer mockMongoServer) {
        return MongoClients.create(mockMongoServer.getConnectionString());
    }
}