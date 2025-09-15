package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the App class
 */
public class AppTest {
    private App app;

    @BeforeEach
    void setUp() {
        app = new App();
    }

    @Test
    void testGetGreeting() {
        String greeting = app.getGreeting();
        assertNotNull(greeting);
        assertEquals("Hello, World!", greeting);
    }

    @Test
    void testProcessInput_withValidInput() {
        String result = app.processInput("hello");
        assertEquals("Processed: HELLO", result);
    }

    @Test
    void testProcessInput_withNullInput() {
        String result = app.processInput(null);
        assertEquals("No input provided", result);
    }

    @Test
    void testProcessInput_withEmptyInput() {
        String result = app.processInput("  ");
        assertEquals("No input provided", result);
    }

    @Test
    void testProcessInput_withMixedCaseInput() {
        String result = app.processInput("Hello World");
        assertEquals("Processed: HELLO WORLD", result);
    }
}