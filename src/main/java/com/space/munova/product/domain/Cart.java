package com.space.munova.product.domain;

import com.space.munova.core.entity.BaseEntity;
import com.space.munova.member.entity.Member;
import com.space.munova.product.application.exception.CartException;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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

        if(quantity <= 0) {
            throw  CartException.badRequestCartException("최소 수량은 1이상 입니다.");
        }

        return Cart.builder()
                .member(member)
                .productDetail(productDetail)
                .quantity(quantity)
                .build();
    }

    ///  장바구니 옵션 변경
    public void updateCart(ProductDetail productDetail, int quantity) {
        
        checkDeletedCart();

        productDetail.checkDeletedProductDetail();

        productDetail.compareInputQauntityAndDetaliQuantity(quantity);

        validInputQuantity(quantity);

        this.productDetail = productDetail;
        this.quantity = quantity;
    }

    /// 장바구니 수량 변경
    public void updateQuantity(int quantity) {

        checkExistItem(quantity);
        validInputQuantity(quantity);
        this.productDetail.compareInputQauntityAndDetaliQuantity(quantity);

        this.quantity = quantity;
    }

    public void checkDeletedCart() {
        if(this.isDeleted) {
            throw CartException.badRequestCartException("제거된 상품입니다.");
        }
    }

    public void checkExistItem(int quantity) {
        if(this.quantity == quantity) {
            throw CartException.badRequestCartException("이미 장바구니에 담긴 상품입니다.");
        }
    }
    
    public void validInputQuantity(int quantity) {
        if(quantity < 1) {
            throw CartException.badRequestCartException("입력 수량은 1보다 작을 수 없습니다.");
        }
    }


}
