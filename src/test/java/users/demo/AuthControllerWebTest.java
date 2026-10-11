package users.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BODY =
            "{\"nom\":\"User1\",\"email\":\"User1@test.com\",\"password\":\"secret123\"}";

    @Test
    void register_newEmail_returns201() throws Exception {
        when(userRepository.findByEmail("User1@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenReturn(new User("User1", "User1@test.com"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("User1@test.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_existingEmail_returns409() throws Exception {
        when(userRepository.findByEmail("User1@test.com"))
                .thenReturn(Optional.of(new User("User1", "User1@test.com")));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict());
    }
}