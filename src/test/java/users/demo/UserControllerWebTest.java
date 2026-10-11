package users.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getUsers_returns200() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of(new User("User1", "User1@test.com")));

        mockMvc.perform(get("/users").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("User1"));
    }

    @Test
    void getUser_withToken_returns200() throws Exception {
        when(userRepository.findById("1"))
                .thenReturn(Optional.of(new User("User1", "User1@test.com")));

        mockMvc.perform(get("/users/1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("User1@test.com"));
    }

    @Test
    void getUsers_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }
}