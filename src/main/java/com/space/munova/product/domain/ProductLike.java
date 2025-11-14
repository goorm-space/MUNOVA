package com.space.munova.product.domain;

import com.space.munova.core.entity.BaseEntity;
import com.space.munova.member.entity.Member;
import com.space.munova.product.application.exception.ProductException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "product_like")
@Builder
public class ProductLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_like_id")
    private Long id;

    @ColumnDefault("0")
    private boolean isDeleted;

    @ManyToOne
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member member;

    @ManyToOne
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Product product;

    public static ProductLike createDefaultProductLike(Product product, Member member) {

        if(product == null || member == null){
            throw ProductException.badRequestException();
        }

        return  ProductLike.builder()
                .product(product)
                .member(member)
                .build();
    }



    public void deleteLike(Long reqMemberId) {
        if(!reqMemberId.equals(this.member.getId())) {
            throw new IllegalArgumentException("유효하지 않은 요청입니다.");
        }

        this.isDeleted = true;
    }
}
