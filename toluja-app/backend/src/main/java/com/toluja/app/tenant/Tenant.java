package com.toluja.app.tenant;

import com.toluja.app.common.BooleanToIntegerConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tenants")
@Getter
@Setter
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tenant_id", nullable = false, unique = true, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Convert(converter = BooleanToIntegerConverter.class)
    @Column(nullable = false)
    private Boolean ativo;
}
