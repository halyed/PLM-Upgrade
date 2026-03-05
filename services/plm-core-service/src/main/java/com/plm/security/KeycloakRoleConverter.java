package com.plm.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Extracts Keycloak realm roles from the JWT claim "realm_access.roles"
 * and maps them to Spring Security ROLE_* GrantedAuthority objects.
 *
 * Keycloak JWT structure:
 * {
 *   "realm_access": {
 *     "roles": ["ADMIN", "ENGINEER", "VIEWER", "offline_access", ...]
 *   }
 * }
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    // Keep standard scopes (e.g. SCOPE_openid) as authorities too
    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> defaultAuthorities = defaultConverter.convert(jwt);

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return defaultAuthorities;
        }

        List<String> roles = (List<String>) realmAccess.get("roles");
        List<GrantedAuthority> realmRoles = roles.stream()
                // Only map PLM application roles; skip Keycloak internal roles
                .filter(role -> role.equals("ADMIN") || role.equals("ENGINEER") || role.equals("VIEWER"))
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .map(GrantedAuthority.class::cast)
                .toList();

        return Stream.concat(defaultAuthorities.stream(), realmRoles.stream()).toList();
    }

    /**
     * Returns a fully configured JwtAuthenticationConverter using this role converter.
     */
    public static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }
}
