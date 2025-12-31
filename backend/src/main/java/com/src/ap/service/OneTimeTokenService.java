package com.src.ap.service;

import com.src.ap.entity.OneTimeToken;
import com.src.ap.entity.TokenType;
import com.src.ap.entity.User;
import com.src.ap.exception.BadRequestException;
import com.src.ap.repository.OneTimeTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

/**
 * Service for managing one-time tokens.
 * Handles token generation, validation, and lifecycle management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OneTimeTokenService {

    private final OneTimeTokenRepository oneTimeTokenRepository;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32; // 256 bits

    /**
     * Generates a cryptographically secure random token.
     * Uses SecureRandom and URL-safe Base64 encoding.
     *
     * @return a secure random token string (URL-safe, ~43 characters)
     */
    public String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Creates and persists a one-time token for a user.
     * Optionally invalidates any previous unused tokens of the same type.
     *
     * @param user the user to create the token for
     * @param type the type of token to create
     * @param invalidatePrevious if true, marks all previous unused tokens of the same type as used
     * @return the created and persisted OneTimeToken
     */
    @Transactional
    public OneTimeToken createToken(User user, TokenType type, boolean invalidatePrevious) {
        // Optionally invalidate previous tokens
        if (invalidatePrevious) {
            invalidatePreviousTokens(user, type);
        }

        // Generate secure token
        String tokenString = generateSecureToken();

        // Create and save new token
        OneTimeToken token = OneTimeToken.builder()
                .token(tokenString)
                .user(user)
                .type(type)
                .used(false)
                .build();

        OneTimeToken savedToken = oneTimeTokenRepository.save(token);
        log.info("Created {} token for user {}: {}", type, user.getUsername(), tokenString);

        return savedToken;
    }

    /**
     * Creates a FIRST_LOGIN token for a user.
     * Invalidates any previous FIRST_LOGIN tokens before creating the new one.
     *
     * @param user the user to create the token for
     * @return the created and persisted OneTimeToken
     */
    @Transactional
    public OneTimeToken createFirstLoginToken(User user) {
        return createToken(user, TokenType.FIRST_LOGIN, true);
    }

    /**
     * Finds a token by its token string.
     *
     * @param tokenString the token string to search for
     * @return the OneTimeToken if found
     * @throws BadRequestException if token not found
     */
    public OneTimeToken findByToken(String tokenString) {
        return oneTimeTokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));
    }

    /**
     * Validates a token for use.
     * Checks that the token exists, is of the correct type, and hasn't been used.
     *
     * @param tokenString the token string to validate
     * @param expectedType the expected token type
     * @return the validated OneTimeToken
     * @throws BadRequestException if token is invalid, wrong type, or already used
     */
    public OneTimeToken validateToken(String tokenString, TokenType expectedType) {
        OneTimeToken token = findByToken(tokenString);

        // Verify token type
        if (token.getType() != expectedType) {
            log.warn("Token type mismatch. Expected: {}, Actual: {}", expectedType, token.getType());
            throw new BadRequestException("Invalid token type");
        }

        // Verify token hasn't been used
        if (token.isUsed()) {
            log.warn("Attempt to use already-consumed token: {}", tokenString);
            throw new BadRequestException("Token has already been used or is no longer valid");
        }

        return token;
    }

    /**
     * Marks a token as used (consumes it).
     *
     * @param token the token to mark as used
     */
    @Transactional
    public void markTokenAsUsed(OneTimeToken token) {
        token.setUsed(true);
        oneTimeTokenRepository.save(token);
        log.info("Marked {} token as used for user {}", token.getType(), token.getUser().getUsername());
    }

    /**
     * Invalidates all previous unused tokens of a specific type for a user.
     * Marks them as used to prevent reuse.
     *
     * @param user the user whose tokens to invalidate
     * @param type the type of tokens to invalidate
     */
    @Transactional
    public void invalidatePreviousTokens(User user, TokenType type) {
        List<OneTimeToken> unusedTokens = oneTimeTokenRepository.findByUserAndTypeAndUsed(user, type, false);

        if (!unusedTokens.isEmpty()) {
            unusedTokens.forEach(token -> token.setUsed(true));
            oneTimeTokenRepository.saveAll(unusedTokens);
            log.info("Invalidated {} previous {} tokens for user {}",
                    unusedTokens.size(), type, user.getUsername());
        }
    }

    /**
     * Gets all tokens for a specific user and type.
     *
     * @param user the user to search for
     * @param type the token type to search for
     * @return list of matching tokens
     */
    public List<OneTimeToken> getTokensByUserAndType(User user, TokenType type) {
        return oneTimeTokenRepository.findByUserAndType(user, type);
    }

    /**
     * Gets all unused tokens for a specific user and type.
     *
     * @param user the user to search for
     * @param type the token type to search for
     * @return list of unused tokens
     */
    public List<OneTimeToken> getUnusedTokensByUserAndType(User user, TokenType type) {
        return oneTimeTokenRepository.findByUserAndTypeAndUsed(user, type, false);
    }
}
