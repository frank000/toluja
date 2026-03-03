package com.toluja.app.superadmin;

import com.toluja.app.user.User;
import com.toluja.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SuperadminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SuperadminService superadminService;

    @Override
    public void run(String... args) {
        superadminService.garantirTenantPadrao();

        if (userRepository.findByUsernameAndTenantId("superadmin", "default").isPresent()) {
            return;
        }

        User superadmin = new User();
        superadmin.setTenantId("default");
        superadmin.setUsername("superadmin");
        superadmin.setNomeExibicao("Super Admin");
        superadmin.setRole("ADMIN");
        superadmin.setPasswordHash(passwordEncoder.encode("Juliet.00"));
        superadmin.setAtivo(true);
        superadmin.setDeveTrocarSenha(false);
        userRepository.save(superadmin);
    }
}
