package com.duoc.cursos.service;
import com.duoc.cursos.model.Usuario;
import com.duoc.cursos.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class UsuarioCognitoNameTest {
    UsuarioRepository repository;
    PerfilUsuarioWriter writer;
    static final String ISSUER = "https://example.com/pool";
    @BeforeEach void setup() {
        repository = mock(UsuarioRepository.class);
        writer = new PerfilUsuarioWriter(repository);
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }
    @Test void nameAndEmailComeFromVerifiedProfile() {
        var user = writer.sincronizar(ISSUER,"subject", "Ana Rojas", "ana@example.com", Set.of("PARTICIPANTE"));
        assertEquals("Ana Rojas", user.getNombre());
        assertEquals(ISSUER, user.getCognitoIssuer());
    }
    @Test void missingNameNeverFallsBackToIdentifier() {
        assertEquals("Nombre no disponible", writer.sincronizar(ISSUER,"subject",null,null,Set.of()).getNombre());
    }
    @Test void oldUuidNameIsHiddenInResponses() {
        var user = new Usuario(); user.setCognitoSub("subject"); user.setNombre("subject");
        assertEquals("Nombre no disponible", new UsuarioService(repository).nombreVisible(user));
    }
    @Test void storedNameSurvivesTemporaryCognitoFailureAndRolesAreRevoked() {
        var user = new Usuario(); user.setId(1L); user.setCognitoSub("subject");
        user.setCognitoIssuer(ISSUER); user.setNombre("Ana Rojas"); user.setEmail("ana@example.com");
        user.setRoles(new HashSet<>(Set.of("DOCENTE")));
        when(repository.findByCognitoSubAndCognitoIssuer("subject",ISSUER)).thenReturn(Optional.of(user));
        var result = writer.sincronizar(ISSUER,"subject",null,null,Set.of());
        assertEquals("Ana Rojas", result.getNombre());
        assertEquals(Set.of("PARTICIPANTE"), result.getRoles());
    }
    @Test void emailCannotRebindAnotherIdentity() {
        var other = new Usuario(); other.setId(77L); other.setCognitoSub("other");
        when(repository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(other));
        assertEquals(409, assertThrows(ResponseStatusException.class, () ->
            writer.sincronizar(ISSUER,"attacker", "Ana", "ana@example.com", Set.of())).getStatusCode().value());
        verify(repository,never()).saveAndFlush(any());
        assertEquals("other", other.getCognitoSub());
    }
}
