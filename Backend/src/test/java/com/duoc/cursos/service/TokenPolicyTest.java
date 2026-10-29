package com.duoc.cursos.service;
import com.duoc.cursos.security.CognitoAccessTokenValidator;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.junit.jupiter.api.Assertions.*;
class TokenPolicyTest {
    private final CognitoAccessTokenValidator validator = new CognitoAccessTokenValidator("our-client");
    private Jwt token(String use, String client) {
        var builder = Jwt.withTokenValue("synthetic").header("alg","RS256").subject("subject").claim("token_use", use);
        if (client != null) builder.claim("client_id",client);
        return builder.build();
    }
    @Test void missingClientIsRejected() { assertTrue(validator.validate(token("access",null)).hasErrors()); }
    @Test void wrongClientIsRejected() { assertTrue(validator.validate(token("access","other")).hasErrors()); }
    @Test void idTokenIsRejected() { assertTrue(validator.validate(token("id","our-client")).hasErrors()); }
    @Test void validAccessClaimsAreAccepted() { assertFalse(validator.validate(token("access","our-client")).hasErrors()); }
    @Test void androidAndWebClientsAreAcceptedExactly() {
        var multiple = new CognitoAccessTokenValidator(" android-client, web-client ");
        assertFalse(multiple.validate(token("access", "android-client")).hasErrors());
        assertFalse(multiple.validate(token("access", "web-client")).hasErrors());
        assertTrue(multiple.validate(token("access", "web")).hasErrors());
        assertTrue(multiple.validate(token("id", "web-client")).hasErrors());
    }
    @Test void emptyAllowlistFailsClosed() {
        assertTrue(new CognitoAccessTokenValidator(" , ").validate(token("access", "our-client")).hasErrors());
        assertTrue(new CognitoAccessTokenValidator(null).validate(token("access", "our-client")).hasErrors());
    }
}
