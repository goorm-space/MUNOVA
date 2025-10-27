package com.space.munova.product.domain;


import com.space.munova.core.entity.BaseEntity;
import com.space.munova.product.exception.ProductDetailException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

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


    public static ProductDetail createDefaultProductDetail(Product product, Integer quantity) {

        if(quantity == null) {
            throw new IllegalArgumentException("quantity cannot be null");
        }
        if(product == null) {
            throw new IllegalArgumentException("product cannot be null");
        }
        if(quantity < 1) {
            throw new IllegalArgumentException("quantity cannot be less than 1");
        }

        return ProductDetail.builder()
                .product(product)
                .quantity(quantity)
                .build();
    }

    public void deductStock(int quantity) {
        if (this.quantity < quantity) {
            throw ProductDetailException.stockInsufficientException("재고 차감 오류: 재고가 부족합니다.");
        }
        this.quantity -= quantity;
    }
}
