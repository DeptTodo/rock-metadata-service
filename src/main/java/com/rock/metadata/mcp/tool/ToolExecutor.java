package com.rock.metadata.mcp.tool;

import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.Callable;

/**
 * Utility to execute MCP tool logic with consistent exception handling.
 * Ensures the Spring AI MCP framework always receives IllegalArgumentException.
 */
final class ToolExecutor {

    private ToolExecutor() {}

    static <T> T run(String action, Callable<T> callable) {
        try {
            return callable.call();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (ResponseStatusException e) {
            throw new IllegalArgumentException(e.getReason());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to " + action + ": " + e.getMessage());
        }
    }

    static void runVoid(String action, Runnable runnable) {
        try {
            runnable.run();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            if (e instanceof ResponseStatusException rse) {
                throw new IllegalArgumentException(rse.getReason());
            }
            throw new IllegalArgumentException("Failed to " + action + ": " + e.getMessage());
        }
    }
}
