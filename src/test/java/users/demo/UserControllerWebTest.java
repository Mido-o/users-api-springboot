package users.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Test
    void deleteUser_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/users/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_asAdmin_returns200() throws Exception {
        mockMvc.perform(delete("/users/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void updateUser_asOwner_returns200() throws Exception {
        when(userRepository.findById("1")).thenReturn(Optional.of(new User("A", "a@test.com")));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(put("/users/1")
                        .with(jwt().jwt(j -> j.subject("1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"B\",\"email\":\"b@test.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateUser_asOtherUser_returns403() throws Exception {
        mockMvc.perform(put("/users/1")
                        .with(jwt().jwt(j -> j.subject("2"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"B\",\"email\":\"b@test.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_asAdmin_returns200() throws Exception {
        when(userRepository.findById("1")).thenReturn(Optional.of(new User("A", "a@test.com")));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(put("/users/1")
                        .with(jwt().jwt(j -> j.subject("99"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"B\",\"email\":\"b@test.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void setRole_selfDemotion_returns409() throws Exception {
        mockMvc.perform(put("/users/1/role")
                        .with(jwt().jwt(j -> j.subject("1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void setRole_lastAdmin_returns409() throws Exception {
        User admin = new User("A", "a@test.com");
        admin.setRole("ADMIN");
        when(userRepository.findById("2")).thenReturn(Optional.of(admin));
        when(userRepository.countByRole("ADMIN")).thenReturn(1L);

        mockMvc.perform(put("/users/2/role")
                        .with(jwt().jwt(j -> j.subject("1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isConflict());
    }
}