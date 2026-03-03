package com.toluja.app.user;

import com.toluja.app.common.BooleanToIntegerConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_tenant_username", columnNames = {"tenant_id", "username"})
        }
)
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Column(name = "nome_exibicao", nullable = false, length = 120)
    private String nomeExibicao;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Convert(converter = BooleanToIntegerConverter.class)
    @Column(nullable = false)
    private Boolean ativo;

    @Convert(converter = BooleanToIntegerConverter.class)
    @Column(name = "deve_trocar_senha", nullable = false)
    private Boolean deveTrocarSenha;
}
