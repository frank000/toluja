package com.toluja.app.item;

import com.toluja.app.common.BooleanToIntegerConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "subitems")
@Getter
@Setter
public class Subitem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id")
    private SubitemCategory categoria;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Convert(converter = BooleanToIntegerConverter.class)
    @Column(nullable = false)
    private Boolean ativo;
}
