package com.duoc.cursos.repository;

import com.duoc.cursos.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCognitoSub(String cognitoSub);

    Optional<Usuario> findByCognitoSubAndCognitoIssuer(String sub, String issuer);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
