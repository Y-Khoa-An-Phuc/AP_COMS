package com.src.ap.repository;

import com.src.ap.entity.OneTimeToken;
import com.src.ap.entity.TokenType;
import com.src.ap.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing one-time tokens.
 * Provides methods to find tokens by token string, user, and type.
 */
@Repository
public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, Long> {

    /**
     * Find a token by its token string.
     *
     * @param token the token string to search for
     * @return Optional containing the OneTimeToken if found
     */
    Optional<OneTimeToken> findByToken(String token);

    /**
     * Find all tokens for a specific user and token type.
     * Useful for cleanup or validation operations.
     *
     * @param user the user to search for
     * @param type the token type to search for
     * @return List of matching tokens
     */
    List<OneTimeToken> findByUserAndType(User user, TokenType type);

    /**
     * Find unused tokens for a specific user and token type.
     * Useful for checking if a user already has an active token.
     *
     * @param user the user to search for
     * @param type the token type to search for
     * @param used whether the token has been used
     * @return List of matching tokens
     */
    List<OneTimeToken> findByUserAndTypeAndUsed(User user, TokenType type, boolean used);
}
