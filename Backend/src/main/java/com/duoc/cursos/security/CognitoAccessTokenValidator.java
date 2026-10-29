package com.duoc.cursos.security;

import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Additional Cognito checks; issuer, timestamps and signature remain validated by Nimbus. */
public final class CognitoAccessTokenValidator implements OAuth2TokenValidator<Jwt> {
    private final Set<String> allowedClientIds;
    public CognitoAccessTokenValidator(String clientIds) {
        this.allowedClientIds = clientIds == null ? Set.of() : Arrays.stream(clientIds.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }
    @Override public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (!"access".equals(jwt.getClaimAsString("token_use")))
            return invalid("Se requiere un access token");
        String clientId = jwt.getClaimAsString("client_id");
        if (clientId == null || !allowedClientIds.contains(clientId))
            return invalid("El client_id del token no coincide");
        return OAuth2TokenValidatorResult.success();
    }
    private OAuth2TokenValidatorResult invalid(String message) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", message, null));
    }
}
