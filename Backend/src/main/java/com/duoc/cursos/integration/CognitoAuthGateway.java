package com.duoc.cursos.integration;


import com.duoc.cursos.security.CognitoRoles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class CognitoAuthGateway {

    public record Registro(String sub) {}

    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${aws.cognito.client-id}")
    private String clientId;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    @Value("${aws.cognito.client-secret:}")
    private String clientSecret;

    public CognitoAuthGateway(CognitoIdentityProviderClient cognitoClient) {
        this.cognitoClient = cognitoClient;
    }

    /**
     * FLUJO: REGISTRO Y COPIA LOCAL CON MÚLTIPLES ROLES
     */
    public Registro registrar(String email, String password, String nombre, Set<String> roles) {
        // 1. Registro en Cognito
        SignUpRequest.Builder signUpBuilder = SignUpRequest.builder()
                .clientId(clientId)
                .username(email)
                .password(password)
                .userAttributes(
                        AttributeType.builder().name("email").value(email).build(),
                        AttributeType.builder().name("name").value(nombre).build()
                );

        // Agregar SECRET_HASH si hay client secret
        if (clientSecret != null && !clientSecret.isBlank()) {
            signUpBuilder.secretHash(generateSecretHash(email));
        }

        SignUpResponse signUpResponse = cognitoClient.signUp(signUpBuilder.build());
        String cognitoSub = signUpResponse.userSub();

        // 2. Asignación a Grupos en Cognito (Roles)
        Set<String> rolesNormalizados = CognitoRoles.normalizeOrDefault(roles);
        for (String rol : rolesNormalizados) {
            AdminAddUserToGroupRequest groupRequest = AdminAddUserToGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .groupName(rol)
                    .build();
            cognitoClient.adminAddUserToGroup(groupRequest);
        }

        return new Registro(cognitoSub);
    }

    /**
     * FLUJO: INICIO DE SESIÓN (LOGIN) PROXY
     */
    public com.duoc.cursos.dto.ApiDtos.LoginResponse login(String email, String password) {
        InitiateAuthRequest.Builder authBuilder = InitiateAuthRequest.builder()
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .clientId(clientId)
                .authParameters(Map.of(
                        "USERNAME", email,
                        "PASSWORD", password
                ));

        if (clientSecret != null && !clientSecret.isBlank()) {
            authBuilder.authParameters(Map.of(
                    "USERNAME", email,
                    "PASSWORD", password,
                    "SECRET_HASH", generateSecretHash(email)
            ));
        }

        InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authBuilder.build());
        var result = authResponse.authenticationResult();
        if (result == null) throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.CONFLICT,
            "Este usuario requiere un desafio de autenticacion. Completa el acceso desde Cognito Hosted UI.");
        return new com.duoc.cursos.dto.ApiDtos.LoginResponse(result.accessToken(), result.idToken(), result.refreshToken(), result.expiresIn());
    }

    /**
     * FLUJO: CIERRE DE SESIÓN GLOBAL (LOGOUT)
     */
    public void logout(String accessToken) {
        GlobalSignOutRequest signOutRequest = GlobalSignOutRequest.builder()
                .accessToken(accessToken)
                .build();
        cognitoClient.globalSignOut(signOutRequest);
    }

    /**
     * FLUJO: OLVIDÉ MI CONTRASEÑA - PASO 1 (SOLICITUD)
     */
    public void solicitarRecuperacionContrasena(String email) {
        ForgotPasswordRequest.Builder requestBuilder = ForgotPasswordRequest.builder()
                .clientId(clientId)
                .username(email);

        if (clientSecret != null && !clientSecret.isBlank()) {
            requestBuilder.secretHash(generateSecretHash(email));
        }

        cognitoClient.forgotPassword(requestBuilder.build());
    }

    /**
     * FLUJO: OLVIDÉ MI CONTRASEÑA - PASO 2 (CONFIRMACIÓN)
     */
    public void confirmarRecuperacionContrasena(String email, String codigoOtp, String nuevaContrasena) {
        ConfirmForgotPasswordRequest.Builder requestBuilder = ConfirmForgotPasswordRequest.builder()
                .clientId(clientId)
                .username(email)
                .confirmationCode(codigoOtp)
                .password(nuevaContrasena);

        if (clientSecret != null && !clientSecret.isBlank()) {
            requestBuilder.secretHash(generateSecretHash(email));
        }

        cognitoClient.confirmForgotPassword(requestBuilder.build());
    }

    /**
     * Genera SECRET_HASH usando HMAC-SHA256 (username + clientId + clientSecret)
     * Requerido cuando el App Client tiene "Generate client secret" activado
     */
    private String generateSecretHash(String username) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = 
                new javax.crypto.spec.SecretKeySpec(clientSecret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal((username + clientId).getBytes());
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generando SECRET_HASH", e);
        }
    }
}