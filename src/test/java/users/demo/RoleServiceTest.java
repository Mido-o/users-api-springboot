package users.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleService service;

    @Test
    void demoteLastAdmin_throws() {
        User admin = new User("A", "a@test.com");
        admin.setRole("ADMIN");
        when(userRepository.findById("2")).thenReturn(Optional.of(admin));
        when(userRepository.countByRole("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> service.changeRole("2", "USER", "1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("last admin");
    }

    @Test
    void selfDemotion_throws() {
        assertThatThrownBy(() -> service.changeRole("1", "USER", "1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot demote themselves");
    }
}