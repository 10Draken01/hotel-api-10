package com.equipouno.auth.configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import com.equipouno.auth.entities.Rol;
import com.equipouno.auth.entities.Usuario;
import com.equipouno.auth.repositories.RolRepository;
import com.equipouno.auth.repositories.UsuarioRepository;
import com.equipouno.auth.services.CustomUserDetails;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configura el SecurityFilterChain para el Authorization Server (OAuth2 + OIDC).
     *
     * - Protege los endpoints del servidor de autorización.
     * - Habilita OpenID Connect (OIDC).
     * - Requiere autenticación para cualquier petición.
     * - Redirige a /login si el usuario no está autenticado.
     */
    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
                // Aplica esta configuración solo a los endpoints del Authorization Server
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, (authorizationServer) ->
                        authorizationServer
                                .oidc(Customizer.withDefaults()) // Habilita OpenID Connect 1.0
                )
                .authorizeHttpRequests((authorize) ->
                        authorize
                                .anyRequest().authenticated() // Todo requiere autenticación
                )
                .exceptionHandling((exceptions) -> exceptions
                        // Si el cliente espera HTML y no está autenticado → redirige a /login
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                );

        return http.build();
    }

    /**
     * Configuración de seguridad principal de la aplicación.
     *
     * - Permite acceso libre a /api/login
     * - Requiere rol ADMIN para /admin/**
     * - Requiere autenticación para el resto
     * - Desactiva CSRF (útil para APIs REST)
     * - Configura CORS para permitir peticiones desde Angular (localhost:4200)
     * - Configura la aplicación como Resource Server usando JWT
     */
    @Bean
    @Order(2)
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/api/login").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable) // Se desactiva CSRF para API REST
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration corsConfiguration = new CorsConfiguration();
                    corsConfiguration.setAllowedOrigins(List.of("http://localhost:4200"));
                    corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    corsConfiguration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
                    corsConfiguration.setAllowCredentials(true);
                    return corsConfiguration;
                }))
                // Configura la app como Resource Server que valida JWT
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    /**
     * Define el servicio que carga los usuarios desde base de datos.
     * Usa tu implementación personalizada CustomUserDetails.
     */
    @Bean
    UserDetailsService userDetailsService(CustomUserDetails customUserDetails) {
        return customUserDetails;
    }

    /**
     * Define el codificador de contraseñas.
     * Se utiliza BCrypt para almacenar contraseñas de forma segura.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Personaliza cómo se extraen los roles desde el JWT.
     *
     * - Lee los roles desde el claim "roles".
     * - Elimina el prefijo automático "ROLE_" para que coincida con tu base de datos.
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    /**
     * Registra un cliente OAuth2 en memoria.
     *
     * - Define clientId y clientSecret.
     * - Usa Authorization Code + Refresh Token.
     * - Define URIs de redirección.
     * - Habilita scopes OpenID y Profile.
     * - Requiere consentimiento del usuario.
     */
    @Bean
    RegisteredClientRepository registeredClientRepository() {

        RegisteredClient oidcClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("oidc-client")
                .clientSecret("{noop}secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/oidc-client")
                .postLogoutRedirectUri("http://127.0.0.1:8080/")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .build();

        return new InMemoryRegisteredClientRepository(oidcClient);
    }

    /**
     * Genera un par de claves RSA para firmar los JWT.
     *
     * - Crea una clave pública y privada.
     * - Se usa para firmar y validar tokens.
     */
    @Bean
    JWKSource<SecurityContext> jwkSource() {

        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);

        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * Genera un KeyPair RSA de 2048 bits.
     * Se usa para firmar los tokens JWT.
     */
    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Configura el decodificador JWT usando la clave pública.
     * Permite validar la firma de los tokens.
     */
    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * Configuración general del Authorization Server.
     * Aquí podrías personalizar issuer, endpoints, etc.
     */
    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    /**
     * Inicializa datos en la base de datos al arrancar la aplicación.
     *
     * - Crea roles ROLE_ADMIN y ROLE_USER si no existen.
     * - Crea usuario admin/admin con rol ADMIN.
     * - Crea usuario usuario/usuario con rol USER.
     */
    @Bean
    CommandLineRunner initData(UsuarioRepository userRepo,
                               RolRepository rolRepo,
                               PasswordEncoder encoder) {

        return args -> {

            Rol adminRole = rolRepo.findByNombre("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Rol r = new Rol();
                        r.setNombre("ROLE_ADMIN");
                        return rolRepo.save(r);
                    });

            Rol userRole = rolRepo.findByNombre("ROLE_USER")
                    .orElseGet(() -> {
                        Rol r = new Rol();
                        r.setNombre("ROLE_USER");
                        return rolRepo.save(r);
                    });

            Rol panchitoRole = rolRepo.findByNombre("ROLE_PANCHITO")
                    .orElseGet(() -> {
                        Rol r = new Rol();
                        r.setNombre("ROLE_PANCHITO");
                        return rolRepo.save(r);
                    });

            if (userRepo.findByUsername("admin").isEmpty()) {
                Usuario admin = new Usuario();
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("admin"));
                admin.setRoles(Set.of(adminRole));
                userRepo.save(admin);
            }

            if (userRepo.findByUsername("usuario").isEmpty()) {
                Usuario user = new Usuario();
                user.setUsername("usuario");
                user.setPassword(encoder.encode("usuario"));
                user.setRoles(Set.of(userRole));
                userRepo.save(user);
            }

            if (userRepo.findByUsername("panchito").isEmpty()) {
                Usuario admin = new Usuario();
                admin.setUsername("panchito");
                admin.setPassword(encoder.encode("panchito"));
                admin.setRoles(Set.of(panchitoRole));
                userRepo.save(admin);
            }
        };
    }
}

