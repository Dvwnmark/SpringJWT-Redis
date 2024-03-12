package com.bkz.dvn.security.api.filter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bkz.dvn.repository.RolePermissionRepository;
import com.bkz.dvn.repository.UserRepository;
import com.bkz.dvn.security.Security;
import com.bkz.dvn.security.api.token.ApiTokenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;


public class ApiTokenAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter{
    private static final Logger logger = LoggerFactory.getLogger(ApiTokenAuthenticationProcessingFilter.class);

    public ApiTokenAuthenticationProcessingFilter(
            Security.SkipMatcher skipMatcher
            , UserRepository userRepository
            , RolePermissionRepository rolePermissionRepository
            , SecurityUserLoginHandler securityUserLoginHandler
            , ApiTokenFactory apiTokenFactory) {
        super(skipMatcher);
        this.userRepository = userRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.securityUserLoginHandler = securityUserLoginHandler;
        this.apiTokenFactory = apiTokenFactory;
    }

    private UserRepository userRepository;

    private RolePermissionRepository rolePermissionRepository;

    private SecurityUserLoginHandler securityUserLoginHandler;

    private ApiTokenFactory apiTokenFactory;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        String accessToken = apiTokenFactory.extract(request.getHeader(ApiTokenData.AUTHORIZATION_HEADER_NAME));
        Map<String, Object> body = apiTokenFactory.getBody(accessToken);
        String principal = (String) body.get("sub"); // username

        /*
         * Do not use granted authority roles in token. (Must use the data that is stored in database or other storage.)
         * Granted authority can be changed by administrator at any time.
         */
        User user = userRepository.findFirstByUsername(principal);
        if(user == null){
            throw new UsernameNotFoundException("User is not exist!");
        }

        // Use the authorities of the user saved in database.
        List<Role> roles = user.getRoles();
        if (roles.isEmpty()) {
            throw new BadCredentialsException("Authentication Failed. User granted authority is empty.");
        }

        List<Long> roleIds = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toList());

        List<String> permissions = rolePermissionRepository.permissions(roleIds);

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        permissions.stream()
                .forEach(p -> grantedAuthorities.add(new SimpleGrantedAuthority(p)));

        logger.info("Api user attempt authentication. username={}, grantedAuthorities={}", principal, grantedAuthorities);
        return new UsernamePasswordAuthenticationToken(principal, null, grantedAuthorities); // No required credentials.
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        securityUserLoginHandler.onAuthenticationFailure(request, response, failed);
    }

}
