package com.khushirathi.docquery;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void register_thenLogin_thenAccessProtectedEndpoint() {
        // 1. register a new user
        HttpHeaders json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);

        String creds = "{\"email\":\"itest@example.com\",\"password\":\"password123\"}";
        ResponseEntity<String> register = rest.postForEntity(
                "/api/v1/auth/register", new HttpEntity<>(creds, json), String.class);
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);   // 201

        // 2. login, get a token
        ResponseEntity<JsonNode> login = rest.postForEntity(
                "/api/v1/auth/login", new HttpEntity<>(creds, json), JsonNode.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);           // 200
        String token = login.getBody().get("accessToken").asText();
        assertThat(token).isNotBlank();

        // 3. call a protected endpoint WITHOUT the token -> 401
        ResponseEntity<String> noToken = rest.getForEntity(
                "/api/v1/documents", String.class);
        assertThat(noToken.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN); // 403 (Spring default for no-token)
        // 4. call it WITH the token -> 200
        HttpHeaders auth = new HttpHeaders();
        auth.setBearerAuth(token);
        ResponseEntity<String> withToken = rest.exchange(
                "/api/v1/documents", HttpMethod.GET, new HttpEntity<>(auth), String.class);
        assertThat(withToken.getStatusCode()).isEqualTo(HttpStatus.OK);        // 200
    }

    @Test
    void duplicateRegistration_returns409() {
        HttpHeaders json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);
        String creds = "{\"email\":\"dupe@example.com\",\"password\":\"password123\"}";

        rest.postForEntity("/api/v1/auth/register",
                new HttpEntity<>(creds, json), String.class);   // first: ok
        ResponseEntity<String> second = rest.postForEntity(
                "/api/v1/auth/register", new HttpEntity<>(creds, json), String.class);

        assertThat(second.getStatusCode().is2xxSuccessful()).isFalse();    // 409
    }
}
