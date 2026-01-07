package com.bitetogether.user.configuration.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SecurityPropertiesTest {

  private SecurityProperties securityProperties;

  @BeforeEach
  void setUp() {
    securityProperties = new SecurityProperties();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "  ", "\t", "\n"})
  void getPermitPaths_WithNullOrBlank_ReturnsEmptyList(String input) {
    securityProperties.setPermitPaths(input);

    List<String> result = securityProperties.getPermitPaths();

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void getPermitPaths_WithSingleValue_ReturnsSingleItemList() {
    securityProperties.setPermitPaths("/api/auth/**");

    List<String> result = securityProperties.getPermitPaths();

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("/api/auth/**", result.get(0));
  }

  @Test
  void getPermitPaths_WithMultipleValues_ReturnsCorrectList() {
    securityProperties.setPermitPaths("/api/auth/**,/api/public/**,/swagger-ui/**");

    List<String> result = securityProperties.getPermitPaths();

    assertNotNull(result);
    assertEquals(3, result.size());
    assertEquals("/api/auth/**", result.get(0));
    assertEquals("/api/public/**", result.get(1));
    assertEquals("/swagger-ui/**", result.get(2));
  }

  @Test
  void getPermitPaths_WithWhitespaceAroundCommas_TrimsCorrectly() {
    securityProperties.setPermitPaths("/api/auth/** , /api/public/** ,  /swagger-ui/**");

    List<String> result = securityProperties.getPermitPaths();

    assertNotNull(result);
    assertEquals(3, result.size());
    assertEquals("/api/auth/**", result.get(0));
    assertEquals("/api/public/**", result.get(1));
    assertEquals("/swagger-ui/**", result.get(2));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "  "})
  void getAllowedOrigins_WithNullOrBlank_ReturnsEmptyList(String input) {
    securityProperties.setAllowedOrigins(input);

    List<String> result = securityProperties.getAllowedOrigins();

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void getAllowedOrigins_WithMultipleValues_ReturnsCorrectList() {
    securityProperties.setAllowedOrigins("http://localhost:3000,https://example.com");

    List<String> result = securityProperties.getAllowedOrigins();

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("http://localhost:3000", result.get(0));
    assertEquals("https://example.com", result.get(1));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "  "})
  void getAllowedMethods_WithNullOrBlank_ReturnsEmptyList(String input) {
    securityProperties.setAllowedMethods(input);

    List<String> result = securityProperties.getAllowedMethods();

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void getAllowedMethods_WithMultipleValues_ReturnsCorrectList() {
    securityProperties.setAllowedMethods("GET,POST,PUT,DELETE");

    List<String> result = securityProperties.getAllowedMethods();

    assertNotNull(result);
    assertEquals(4, result.size());
    assertEquals("GET", result.get(0));
    assertEquals("POST", result.get(1));
    assertEquals("PUT", result.get(2));
    assertEquals("DELETE", result.get(3));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "  "})
  void getAllowedHeaders_WithNullOrBlank_ReturnsEmptyList(String input) {
    securityProperties.setAllowedHeaders(input);

    List<String> result = securityProperties.getAllowedHeaders();

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void getAllowedHeaders_WithMultipleValues_ReturnsCorrectList() {
    securityProperties.setAllowedHeaders("Authorization,Content-Type,X-Custom-Header");

    List<String> result = securityProperties.getAllowedHeaders();

    assertNotNull(result);
    assertEquals(3, result.size());
    assertEquals("Authorization", result.get(0));
    assertEquals("Content-Type", result.get(1));
    assertEquals("X-Custom-Header", result.get(2));
  }

  @Test
  void getAllowedHeaders_WithSingleValue_ReturnsSingleItemList() {
    securityProperties.setAllowedHeaders("Authorization");

    List<String> result = securityProperties.getAllowedHeaders();

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("Authorization", result.get(0));
  }

  @Test
  void getAllMethods_WithComplexWhitespace_ParsesCorrectly() {
    securityProperties.setPermitPaths("/api/auth/** ,  /api/public/**  , /swagger-ui/**");
    securityProperties.setAllowedOrigins("http://localhost:3000 , https://example.com");
    securityProperties.setAllowedMethods("GET , POST , PUT");
    securityProperties.setAllowedHeaders("Authorization , Content-Type");

    assertEquals(3, securityProperties.getPermitPaths().size());
    assertEquals(2, securityProperties.getAllowedOrigins().size());
    assertEquals(3, securityProperties.getAllowedMethods().size());
    assertEquals(2, securityProperties.getAllowedHeaders().size());
  }

  @Test
  void allowCredentials_GetterSetter_WorksCorrectly() {
    securityProperties.setAllowCredentials(true);

    assertTrue(securityProperties.isAllowCredentials());

    securityProperties.setAllowCredentials(false);

    assertEquals(false, securityProperties.isAllowCredentials());
  }

  @ParameterizedTest
  @MethodSource("provideSplitTestCases")
  void split_WithVariousInputs_ReturnsExpectedResults(
      String input, int expectedSize, List<String> expectedValues) {
    securityProperties.setPermitPaths(input);

    List<String> result = securityProperties.getPermitPaths();

    assertEquals(expectedSize, result.size());
    if (!expectedValues.isEmpty()) {
      assertEquals(expectedValues, result);
    }
  }

  private static Stream<Arguments> provideSplitTestCases() {
    return Stream.of(
        Arguments.of("single", 1, List.of("single")),
        Arguments.of("one,two", 2, List.of("one", "two")),
        Arguments.of("a,b,c,d,e", 5, List.of("a", "b", "c", "d", "e")),
        Arguments.of("no-comma", 1, List.of("no-comma")),
        Arguments.of(null, 0, List.of()),
        Arguments.of("", 0, List.of()),
        Arguments.of("   ", 0, List.of()));
  }
}
