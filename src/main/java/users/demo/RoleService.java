package users.demo;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
public class RoleService {

    private final UserRepository userRepository;

    public RoleService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private void requireAnotherAdmin() {
        if (userRepository.countByRole("ADMIN") <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot remove the last admin");
        }
    }

    // synchronized lets only one thread inside at a time.
    public synchronized User changeRole(String id, String newRole, String callerId) {
        if (!Set.of("USER", "ADMIN").contains(newRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be USER or ADMIN");
        }
        if (id.equals(callerId) && !"ADMIN".equals(newRole)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admins cannot demote themselves");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if ("ADMIN".equals(user.getRole()) && !"ADMIN".equals(newRole)) {
            requireAnotherAdmin();
        }

        user.setRole(newRole);

        return userRepository.save(user);
    }

    public synchronized void deleteUser(String id) {
        userRepository.findById(id).ifPresent(user -> {
            if ("ADMIN".equals(user.getRole())) {
                requireAnotherAdmin();
            }
            userRepository.deleteById(id);
        });
    }
}