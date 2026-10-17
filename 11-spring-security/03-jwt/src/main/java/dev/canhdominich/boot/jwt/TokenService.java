package dev.canhdominich.boot.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final JwtEncoder encoder;

    public TokenService(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public String issue(String username, List<? extends GrantedAuthority> authorities, Duration lifetime) {
        return issueAt(username, authorities, Instant.now(), lifetime);
    }

    public String issueAt(String username, List<? extends GrantedAuthority> authorities, Instant issuedAt, Duration lifetime) {
        List<String> roles = authorities.stream().map(GrantedAuthority::getAuthority).map(a -> a.startsWith("ROLE_") ? a.substring(5) : a).toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(username)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(lifetime))
                .claim("roles", roles)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
