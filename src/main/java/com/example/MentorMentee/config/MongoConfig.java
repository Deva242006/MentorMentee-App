package com.example.MentorMentee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

/**
 * Provides a {@link GridFsTemplate} for document storage. Declared explicitly (rather than relying on
 * auto-configuration) so the documents feature works regardless of Boot's GridFS auto-config state;
 * both collaborators are themselves auto-configured by Spring Data MongoDB.
 */
@Configuration
public class MongoConfig {

    @Bean
    GridFsTemplate gridFsTemplate(MongoDatabaseFactory factory, MappingMongoConverter converter) {
        return new GridFsTemplate(factory, converter);
    }
}
