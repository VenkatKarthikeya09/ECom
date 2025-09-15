package com.example.ecom.repository;

import com.example.ecom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entities.
 * This extends JpaRepository to provide standard database operations.
 *
 * @author Gemini
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Finds a User by their username.
     * Spring Data JPA automatically generates the query for this method.
     *
     * @param username The username to search for.
     * @return An Optional containing the User if found, otherwise an empty Optional.
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a User by their email address.
     *
     * @param email The email address to search for.
     * @return An Optional containing the User if found, otherwise an empty Optional.
     */
    Optional<User> findByEmail(String email);
}
