package com.cuongnn.tutoringappserver.common.security;

import com.cuongnn.tutoringappserver.common.exception.ServiceStatus;
import com.cuongnn.tutoringappserver.entities.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.cuongnn.tutoringappserver.common.utils.Constants.AUTH;
import static com.cuongnn.tutoringappserver.common.utils.Constants.AUTHORIZATION;
import static com.cuongnn.tutoringappserver.common.utils.Constants.BEARER;
import static com.cuongnn.tutoringappserver.common.utils.Constants.EXPIRE_LENGTH;
import static com.cuongnn.tutoringappserver.common.utils.Constants.INVALID_TOKEN;
import static com.cuongnn.tutoringappserver.common.utils.Constants.SECRET_KEY;

@Component
public class JwtTokenProvider {

    private final CustomUserDetail userDetail;

    @Autowired
    public JwtTokenProvider(CustomUserDetail userDetail) {
        this.userDetail = userDetail;
    }

    @PostConstruct
    private void init() {
        SECRET_KEY = Base64.getEncoder().encodeToString(SECRET_KEY.getBytes());
    }

    public String createToken(String username, List<Role> roles) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put(AUTH, roles.stream()
                .map(s -> new SimpleGrantedAuthority(s.getAuthority()))
                .collect(Collectors.toList()));

        Date now = new Date();
        Date validity = new Date(now.getTime() + EXPIRE_LENGTH);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        UserDetails userDetails = userDetail.loadUserByUsername(getUserName(token));
        return new UsernamePasswordAuthenticationToken(userDetails, Strings.EMPTY, userDetails.getAuthorities());
    }

    public String getUserName(String token) {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJwt(token).getBody().getSubject();
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION);
        return (Objects.nonNull(bearerToken) && bearerToken.startsWith(BEARER))
                ? bearerToken.substring(7) : null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJwt(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            throw new ServiceStatus(INVALID_TOKEN, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
