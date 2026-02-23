package com.miracle.smart_ecommerce_jpa.domain.user.service.impl;

import com.miracle.smart_ecommerce_jpa.domain.user.dto.request.CreateUserRequest;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.response.UserResponse;
import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;
import com.miracle.smart_ecommerce_jpa.domain.user.repository.UserRepository;
import com.miracle.smart_ecommerce_jpa.exception.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(passwordEncoder.encode(any(String.class))).thenReturn("hashed");
    }

    @Test
    void createUser_success() {
        CreateUserRequest req = CreateUserRequest.builder()
                .emailAddress("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .password("password123")
                .build();

        when(userRepository.existsByEmailAddress(req.getEmailAddress())).thenReturn(false);

        UUID id = UUID.randomUUID();
        User saved = User.builder()
                .id(id)
                .emailAddress(req.getEmailAddress())
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .phoneNumber(req.getPhoneNumber())
                .passwordHash("hashed")
                .isActive(true)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponse resp = userService.createUser(req);

        assertNotNull(resp);
        assertEquals(req.getEmailAddress(), resp.getEmailAddress());
        verify(userRepository, times(1)).save(any(User.class));

        // verify saved user had password hashed (we can't assert exact hash but ensure passwordHash not equal to plain)
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User toSave = captor.getValue();
        assertNotNull(toSave.getPasswordHash());
        assertNotEquals(req.getPassword(), toSave.getPasswordHash());
    }

    @Test
    void createUser_duplicateEmail_throws() {
        CreateUserRequest req = CreateUserRequest.builder()
                .emailAddress("dup@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByEmailAddress(req.getEmailAddress())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.createUser(req));

        verify(userRepository, never()).save(any());
    }
}

