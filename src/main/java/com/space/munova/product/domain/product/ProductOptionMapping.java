package com.space.munova.product.domain.product;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "product_option_mapping")
public class ProductOptionMapping  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "option_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Option option;

    @ManyToOne
    @JoinColumn(name = "product_detail_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ProductDetail productDetail;

}
