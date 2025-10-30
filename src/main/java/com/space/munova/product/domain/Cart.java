package com.space.munova.product.domain;

import com.space.munova.core.entity.BaseEntity;
import com.space.munova.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Builder
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "cart")
public class Cart extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long id;

    private int quantity;

    @ColumnDefault("0")
    private boolean isDeleted;

    @ManyToOne
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member member;

    @ManyToOne
    @JoinColumn(name = "product_detail_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ProductDetail productDetail;


    public static Cart createDefaultCart(Member member, ProductDetail productDetail, int quantity) {
        return Cart.builder()
                .member(member)
                .productDetail(productDetail)
                .quantity(quantity)
                .build();
    }

    ///  장바구니 옵션 변경
    public void updateCart(ProductDetail productDetail, int quantity) {
        if(quantity > productDetail.getQuantity()) {
            throw new IllegalArgumentException("수정할 수량은 상품의 수량을 초과할 수 없습니다.");
        }
        if(quantity < 1) {
            throw new IllegalArgumentException("수량은 1 보다 작을 수 없습니다.");
        }
        this.productDetail = productDetail;
        this.quantity = quantity;
    }

    /// 장바구니 수량 변경
    public void updateQuantity(int quantity) {
        if(quantity < 0 || quantity > this.productDetail.getQuantity()) {
            throw new IllegalArgumentException("수량은 0 이상이어야 하고, 상품의 수량을 초과하여 입력할 수 없습니다.");
        }
        this.quantity = quantity;
    }
}
