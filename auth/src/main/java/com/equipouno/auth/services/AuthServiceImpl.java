package com.equipouno.auth.services;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.equipouno.auth.dto.LoginRequest;
import com.equipouno.auth.dto.TokenResponse;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserDetailsService userDetailsService;

    private final RSAKey rsaKey;

    public AuthServiceImpl(UserDetailsService userDetailsService, JWKSource<SecurityContext> jwkSource) {
        this.userDetailsService = userDetailsService;
        try {
            JWKSelector jwkSelector = new JWKSelector(
                    new JWKMatcher.Builder().keyType(KeyType.RSA).build()
            );
            var jwks = jwkSource.get(jwkSelector, null);
            if (jwks == null || jwks.isEmpty())
                throw new RuntimeException("No se pudo obtener la clave RSA");
            rsaKey = (RSAKey) jwks.get(0);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo obtener la clave RSA");
        }
    }

    @Override
    public TokenResponse autenticar(LoginRequest request) throws Exception {
        log.info("Cargando usuario {}", request.username());

        // Cargar el usuario desde la base de datos
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());

        if (userDetails == null || !new BCryptPasswordEncoder().matches(
                request.password(), userDetails.getPassword()) ) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        // Se obtiene el stampt actual de ahora mismismo
        Instant now = Instant.now();

        // Se crea los claims
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                // emisor
                .issuer("http://localhost:9000")
                // el sujeto
                .subject(userDetails.getUsername())
                // el tiempo de emisión
                .issueTime(Date.from(now))
                // el tiempo de expiración (1 hora)
                .expirationTime(Date.from(now.plusSeconds(3600)))
                // el id del token
                .jwtID(UUID.randomUUID().toString())
                // claim personalizado: Este con roles de usuario
                .claim("roles", userDetails.getAuthorities()
                        .stream().map(GrantedAuthority::getAuthority).toList())
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                claimsSet
        );

        JWSSigner signer = new RSASSASigner(rsaKey.toPrivateKey());
        signedJWT.sign(signer);

        return new TokenResponse(signedJWT.serialize());
    }
}