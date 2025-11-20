package com.space.munova.product.domain;


import com.space.munova.core.entity.BaseEntity;
import com.space.munova.product.application.exception.CartException;
import com.space.munova.product.application.exception.ProductDetailException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Table(name = "product_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductDetail extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_detail_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Product product;

    private Integer quantity;

    @ColumnDefault("0")
    private boolean isDeleted;

    @OneToMany(mappedBy = "productDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductOptionMapping>  optionMappings = new ArrayList<>();

    public static ProductDetail createDefaultProductDetail(Product product, Integer quantity) {

        if(quantity == null) {
            throw new IllegalArgumentException("수량은 null일 수 없습니다.");
        }
        if(product == null) {
            throw new IllegalArgumentException("상품은 null일 수 없습니다.");
        }
        if(quantity < 1) {
            throw new IllegalArgumentException("수량은 1보다 작을 수 없습니다.");
        }

        return ProductDetail.builder()
                .product(product)
                .quantity(quantity)
                .build();
    }

    public String getNameSnapshot() {
        return this.product.getName();
    }

    public Long getPriceSnapshot() {
        return this.product.getPrice();
    }

    public void deductStock(int quantity) {
        if (this.quantity < quantity) {
            throw ProductDetailException.stockInsufficientException("재고 차감 오류: 재고가 부족합니다.");
        }
        this.quantity -= quantity;
    }

    public void increaseStock(int quantity) {
        if (this.quantity == null) {
            this.quantity = 0;
        }

        this.quantity += quantity;
    }

    public void validAddToCart(int quantity) {
        if(this.quantity == quantity) {
            throw CartException.badRequestCartException("상품의 수량을 초과하여 상품을 담을 수 없습니다.");
        }
        if(this.isDeleted) {
            throw CartException.badRequestCartException("제거된 상품은 장바구니에 추가할 수 없습니다.");
        }
    }

    public void checkDeletedProductDetail() {
        if(this.isDeleted) {
            throw ProductDetailException.isDeletedProduct();
        }
    }

    public void compareInputQauntityAndDetaliQuantity(int inputQauntity) {
        if(this.quantity < inputQauntity) {
            throw ProductDetailException.badRequest("상품 수량을 초과하여 수량을 입력할 수 없습니다.");
        }
    }
}
