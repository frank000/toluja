package com.toluja.app.auth;

import com.toluja.app.dto.AuthDtos;
import com.toluja.app.security.JwtService;
import com.toluja.app.user.User;
import com.toluja.app.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        User user = userRepository.findByUsernameAndAtivoTrue(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole(), user.getDeveTrocarSenha());
        var userInfo = new AuthDtos.UserInfo(user.getId(), user.getUsername(), user.getNomeExibicao(), user.getRole(), user.getDeveTrocarSenha());
        return new AuthDtos.LoginResponse(token, userInfo);
    }

    @PostMapping("/change-password")
    public AuthDtos.LoginResponse changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest request,
                                                 Authentication authentication) {
        User user = userRepository.findByUsernameAndAtivoTrue(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (!passwordEncoder.matches(request.senhaAtual(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha atual inválida");
        }

        user.setPasswordHash(passwordEncoder.encode(request.novaSenha()));
        user.setDeveTrocarSenha(false);
        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername(), user.getRole(), false);
        var userInfo = new AuthDtos.UserInfo(user.getId(), user.getUsername(), user.getNomeExibicao(), user.getRole(), false);
        return new AuthDtos.LoginResponse(token, userInfo);
    }
}
