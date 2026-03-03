package com.toluja.app.order;

import com.toluja.app.item.Subitem;
import jakarta.persistence.Column;
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
@Table(name = "order_item_subitems")
@Getter
@Setter
public class OrderItemSubitem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "subitem_id")
    private Subitem subitem;

    @Column(name = "categoria_nome_snapshot", nullable = false, length = 80)
    private String categoriaNomeSnapshot;

    @Column(name = "nome_snapshot", nullable = false, length = 80)
    private String nomeSnapshot;

    @Column(name = "preco_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoSnapshot;
}
