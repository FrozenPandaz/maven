package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for simple App.
 */
public class AppTest {
    
    @Test
    public void testGetGreeting() {
        App app = new App();
        assertEquals("Hello World from Maven + Nx!", app.getGreeting());
    }
    
    @Test 
    public void testAppNotNull() {
        App app = new App();
        assertNotNull(app);
    }
}