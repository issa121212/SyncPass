package com.duoc.cursos.service;
import com.duoc.cursos.integration.CognitoAuthGateway;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AuthPolicyTest {
    @Test void publicRegistrationCanOnlyAssignParticipant() {
        var gateway = mock(CognitoAuthGateway.class);
        var writer = mock(PerfilUsuarioWriter.class);
        when(gateway.registrar("a@example.com","password","Ana",Set.of("PARTICIPANTE")))
            .thenReturn(new CognitoAuthGateway.Registro("sub"));
        new AuthService(gateway, writer, mock(UsuarioService.class), "issuer", false).registrar("a@example.com","password","Ana");
        verify(writer).sincronizar("issuer","sub","Ana","a@example.com",Set.of("PARTICIPANTE"));
    }
    @Test void passwordProxyIsDisabledInProduction() {
        var gateway = mock(CognitoAuthGateway.class);
        var service = new AuthService(gateway,mock(PerfilUsuarioWriter.class),mock(UsuarioService.class),"issuer",false);
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.login("a","b")).getStatusCode().value());
        verifyNoInteractions(gateway);
    }
}
