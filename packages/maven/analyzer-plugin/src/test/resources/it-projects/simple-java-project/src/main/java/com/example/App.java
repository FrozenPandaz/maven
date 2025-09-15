package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple application class for testing
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        App app = new App();
        String message = app.getGreeting();
        logger.info("Application started: {}", message);
        System.out.println(message);
    }

    public String getGreeting() {
        return "Hello, World!";
    }

    public String processInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "No input provided";
        }
        return "Processed: " + input.toUpperCase();
    }
}