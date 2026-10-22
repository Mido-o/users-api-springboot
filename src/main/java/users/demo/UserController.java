package users.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/users")
public class UserController {
    record RoleRequest(String role) {}

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.name")
    public Optional<User> getUser(@PathVariable String id) {
        return userRepository.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.name")
    public User updateUser(@PathVariable String id, @RequestBody User user) {
        User existingUser = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        userRepository.findByEmail(user.getEmail())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already used");
                });

        existingUser.setNom(user.getNom());
        existingUser.setEmail(user.getEmail());

        return userRepository.save(existingUser);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable String id) {
        userRepository.findById(id).ifPresent(user -> {
            if ("ADMIN".equals(user.getRole()) && userRepository.countByRole("ADMIN") <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete the last admin");
            }
        });

        userRepository.deleteById(id);
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public User setRole(@PathVariable String id, @RequestBody RoleRequest req, Authentication authentication) {
        if (!Set.of("USER", "ADMIN").contains(req.role())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be USER or ADMIN");

        if (id.equals(authentication.getName()) && !"ADMIN".equals(req.role())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Admins cannot demote themselves");

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!"ADMIN".equals(req.role())
                && "ADMIN".equals(user.getRole())
                && userRepository.countByRole("ADMIN") <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot remove the last admin");
        }

        user.setRole(req.role());

        return userRepository.save(user);
    }
}