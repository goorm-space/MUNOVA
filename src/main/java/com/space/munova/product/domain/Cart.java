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


    /// 장바구니 수량 변경
    public void updateQuantity(int quantity) {
        if(quantity < 0){
            throw new IllegalArgumentException("수량은 0 이상이어야 합니다.");
        }
        this.quantity = quantity;
    }
}
