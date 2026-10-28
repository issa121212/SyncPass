package com.duoc.cursos.service;

import com.duoc.cursos.integration.CognitoProfileService;
import com.duoc.cursos.security.CognitoRoles;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;

/** Provider I/O completes before entering a short, dedicated database transaction. */
@Service
public class SesionUsuarioService {
    private final CognitoProfileService profiles;
    private final PerfilUsuarioWriter writer;
    public SesionUsuarioService(CognitoProfileService profiles, PerfilUsuarioWriter writer) {
        this.profiles = profiles; this.writer = writer;
    }
    public void sincronizar(Jwt jwt) {
        var profile = profiles.profile(jwt);
        var groups = jwt.getClaimAsStringList("cognito:groups");
        writer.sincronizar(jwt.getIssuer() == null ? null : jwt.getIssuer().toString(), jwt.getSubject(),
            profile == null ? jwt.getClaimAsString("name") : profile.name(),
            profile == null ? jwt.getClaimAsString("email") : profile.email(),
            CognitoRoles.normalizeOrDefault(groups == null ? java.util.List.of() : groups));
    }
}
