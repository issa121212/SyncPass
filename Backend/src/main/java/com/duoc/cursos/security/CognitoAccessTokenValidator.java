package com.duoc.cursos.security;

import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;

/** Additional Cognito checks; issuer, timestamps and signature remain validated by Nimbus. */
public final class CognitoAccessTokenValidator implements OAuth2TokenValidator<Jwt> {
    private final String clientId;
    public CognitoAccessTokenValidator(String clientId) { this.clientId = clientId; }
    @Override public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (!"access".equals(jwt.getClaimAsString("token_use")))
            return invalid("Se requiere un access token");
        if (clientId == null || clientId.isBlank() || !clientId.equals(jwt.getClaimAsString("client_id")))
            return invalid("El client_id del token no coincide");
        return OAuth2TokenValidatorResult.success();
    }
    private OAuth2TokenValidatorResult invalid(String message) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", message, null));
    }
}
