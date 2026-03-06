package com.rock.metadata.mcp;

import com.rock.metadata.mcp.tool.CrawlTools;
import com.rock.metadata.mcp.tool.DataSourceTools;
import com.rock.metadata.mcp.tool.MetadataTools;
import com.rock.metadata.mcp.tool.SqlTools;
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
}
