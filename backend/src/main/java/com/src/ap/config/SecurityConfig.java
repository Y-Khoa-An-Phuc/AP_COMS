package com.src.ap.config;

import com.src.ap.config.JwtAuthenticationFilter;
import com.src.ap.security.CustomAccessDeniedHandler;
import com.src.ap.security.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuditRequestContextFilter auditRequestContextFilter;
    private final UserDetailsService userDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Value("${security.password.argon2.saltLength:16}")
    private int argon2SaltLength;

    @Value("${security.password.argon2.hashLength:32}")
    private int argon2HashLength;

    @Value("${security.password.argon2.parallelism:1}")
    private int argon2Parallelism;

    @Value("${security.password.argon2.memory:65536}")
    private int argon2Memory;

    @Value("${security.password.argon2.iterations:3}")
    private int argon2Iterations;

    /**
     * Configures the security filter chain for the application.
     *
     * <p><b>Temporary Password Flow & Access Control:</b></p>
     * Users with temporary passwords (mustChangePassword=true, temporaryPassword=true) are restricted as follows:
     * <ul>
     *   <li>They can ONLY access:
     *     <ul>
     *       <li>POST /api/auth/login - to authenticate and receive 403 Forbidden (no JWT issued)</li>
     *       <li>POST /api/auth/change-temporary-password - to change their temporary password</li>
     *     </ul>
     *   </li>
     *   <li>They CANNOT access any other endpoints because:
     *     <ul>
     *       <li>No JWT token is issued during login if temporary password is detected</li>
     *       <li>All other /api/** endpoints require valid JWT authentication</li>
     *       <li>Without a JWT, they are automatically blocked by Spring Security</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p><b>Security Rules:</b></p>
     * <ul>
     *   <li><b>Public Endpoints (no JWT required):</b>
     *     <ul>
     *       <li>POST /api/auth/login - Publicly accessible for all users</li>
     *       <li>POST /api/auth/change-temporary-password - Publicly accessible for temporary password users</li>
     *     </ul>
     *   </li>
     *   <li><b>Role-Based Endpoints:</b>
     *     <ul>
     *       <li>POST /api/auth/register - Requires TECHADMIN role (user creation)</li>
     *     </ul>
     *   </li>
     *   <li><b>Protected Endpoints (JWT required):</b>
     *     <ul>
     *       <li>All other /api/** endpoints - Require valid JWT authentication</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p><b>Session Management:</b> Stateless (JWT-based authentication, no server-side sessions)</p>
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ============================================================
                        // PUBLIC ENDPOINTS - No JWT Required
                        // ============================================================

                        // Login endpoint - publicly accessible for all users
                        // Users with temporary password will receive 403 with no JWT
                        .requestMatchers("/api/auth/login").permitAll()

                        // Change temporary password - publicly accessible (no JWT required)
                        // Only works if user has both mustChangePassword=true AND temporaryPassword=true
                        .requestMatchers("/api/auth/change-temporary-password").permitAll()

                        // First login endpoints - publicly accessible (no JWT required)
                        // Token-based password setup for new users
                        .requestMatchers("/api/auth/first-login/validate").permitAll()
                        .requestMatchers("/api/auth/first-login/set-password").permitAll()

                        // ============================================================
                        // ROLE-BASED ENDPOINTS
                        // ============================================================

                        // User registration - requires TECHADMIN or SUPERADMIN role
                        .requestMatchers("/api/auth/register").hasAnyRole("TECHADMIN", "SUPERADMIN")

                        // ============================================================
                        // PROTECTED ENDPOINTS - Valid JWT Required
                        // ============================================================

                        // All other API endpoints require authentication (valid JWT)
                        // This automatically blocks temporary password users since they have no JWT
                        .requestMatchers("/api/**").authenticated()

                        // Everything else is permitted (for non-API routes)
                        .anyRequest().permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(auditRequestContextFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
                argon2SaltLength,
                argon2HashLength,
                argon2Parallelism,
                argon2Memory,
                argon2Iterations
        );
    }
}
