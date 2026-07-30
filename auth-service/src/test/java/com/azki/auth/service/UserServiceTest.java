package com.azki.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azki.auth.entity.User;
import com.azki.auth.entity.UserRole;
import com.azki.auth.exception.InvalidCredentialsException;
import com.azki.auth.exception.UsernameAlreadyExistsException;
import com.azki.auth.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void shouldRegisterNewUserWithHashedPasswordAndCustomerRole() {
        // given
        String username = "nima_test";
        String rawPassword = "SecurePass123";
        String hashedPassword = "hashed_password_value";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        User result = userService.register(username, rawPassword);

        // then
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getPasswordHash()).isEqualTo(hashedPassword);
        assertThat(result.getRole()).isEqualTo(UserRole.CUSTOMER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenRegisteringExistingUsername() {
        // given
        String username = "nima_test";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> userService.register(username, "anyPassword"))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining(username);
    }

    @Test
    void shouldAuthenticateUserWithCorrectPassword() {
        // given
        String username = "nima_test";
        String rawPassword = "SecurePass123";
        User existingUser = new User(username, "hashed_value", UserRole.CUSTOMER);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(rawPassword, existingUser.getPasswordHash())).thenReturn(true);

        // when
        User result = userService.authenticate(username, rawPassword);

        // then
        assertThat(result).isEqualTo(existingUser);
    }

    @Test
    void shouldThrowExceptionWhenAuthenticatingWithWrongPassword() {
        // given
        String username = "nima_test";
        User existingUser = new User(username, "hashed_value", UserRole.CUSTOMER);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongPassword", existingUser.getPasswordHash())).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> userService.authenticate(username, "wrongPassword"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void shouldThrowExceptionWhenAuthenticatingNonExistentUser() {
        // given
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> userService.authenticate("ghost", "anyPassword"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

}