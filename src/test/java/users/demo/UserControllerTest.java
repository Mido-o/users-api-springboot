package users.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserController controller;

    @Test
    void getUsers_returnsAll() {
        when(userRepository.findAll()).thenReturn(List.of(new User("User1", "user1@test.com")));

        assertThat(controller.getUsers()).hasSize(1);
    }

    @Test
    void updateUser_changesFields() {
        User existing = new User("Old", "old@test.com");
        when(userRepository.findById("1")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = controller.updateUser("1", new User("New", "new@test.com"));

        assertThat(result.getNom()).isEqualTo("New");
        assertThat(result.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    void updateUser_unknownId_throws() {
        when(userRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.updateUser("x", new User("a", "b")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void deleteUser_callsRepository() {
        controller.deleteUser("1");

        verify(userRepository).deleteById("1");
    }
}