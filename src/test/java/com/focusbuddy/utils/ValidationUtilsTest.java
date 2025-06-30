package com.focusbuddy.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ValidationUtilsTest {
    
    @Test
    public void testValidEmail() {
        assertTrue(ValidationUtils.isValidEmail("test@example.com"));
        assertTrue(ValidationUtils.isValidEmail("user.name@domain.co.uk"));
        assertFalse(ValidationUtils.isValidEmail("invalid-email"));
        assertFalse(ValidationUtils.isValidEmail("@domain.com"));
        assertFalse(ValidationUtils.isValidEmail("user@"));
    }
    
    @Test
    public void testValidUsername() {
        assertTrue(ValidationUtils.isValidUsername("user123"));
        assertTrue(ValidationUtils.isValidUsername("test_user"));
        assertFalse(ValidationUtils.isValidUsername("us")); // too short
        assertFalse(ValidationUtils.isValidUsername("user@name")); // invalid character
        assertFalse(ValidationUtils.isValidUsername("")); // empty
    }
    
    @Test
    public void testValidPassword() {
        assertTrue(ValidationUtils.isValidPassword("password123"));
        assertFalse(ValidationUtils.isValidPassword("12345")); // too short
        assertFalse(ValidationUtils.isValidPassword("")); // empty
        assertFalse(ValidationUtils.isValidPassword(null)); // null
    }
    
    @Test
    public void testSanitizeInput() {
        assertEquals("Hello World", ValidationUtils.sanitizeInput("Hello World"));
        assertEquals("Hello World", ValidationUtils.sanitizeInput("<script>Hello World</script>"));
        assertEquals("Test  Input", ValidationUtils.sanitizeInput("Test <> Input"));
        assertEquals("", ValidationUtils.sanitizeInput(null));
    }
}
