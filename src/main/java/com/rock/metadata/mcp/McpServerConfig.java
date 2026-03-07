package com.rock.metadata.mcp;

import com.rock.metadata.mcp.tool.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    ToolCallbackProvider dataSourceToolProvider(DataSourceTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }

    @Bean
    ToolCallbackProvider crawlToolProvider(CrawlTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }

    @Bean
    ToolCallbackProvider metadataToolProvider(MetadataTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }

    @Bean
    ToolCallbackProvider sqlToolProvider(SqlTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }

    @Bean
    ToolCallbackProvider tagToolProvider(TagTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }

    @Bean
    ToolCallbackProvider dictToolProvider(DictTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }

    @Bean
    ToolCallbackProvider llmAnalysisToolProvider(LlmAnalysisTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }

    @Bean
    ToolCallbackProvider schemaDiffToolProvider(SchemaDiffTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }

    @Bean
    ToolCallbackProvider relationshipToolProvider(RelationshipTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }

    @Bean
    ToolCallbackProvider profilingToolProvider(ProfilingTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }

    @Bean
    ToolCallbackProvider annotationToolProvider(AnnotationTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }

    @Bean
    ToolCallbackProvider dataQualityToolProvider(DataQualityTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }

    @Bean
    ToolCallbackProvider datasetToolProvider(DatasetTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }
}
