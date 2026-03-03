package com.toluja.app.order;

import com.toluja.app.item.Item;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "nome_snapshot", nullable = false)
    private String nomeSnapshot;

    @Column(name = "preco_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoSnapshot;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItemSubitem> subitens = new ArrayList<>();
}
