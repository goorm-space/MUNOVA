package com.space.munova.product.domain.product;


import com.space.munova.product.domain.product.enums.ProductCategory;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "product_category")
@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nullable
    @ManyToOne
    @JoinColumn(name = "ref_product_category_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Category refCategory;

    @Convert(converter = ProductCategoryConverter.class)
    private ProductCategory categoryType;

    private Integer level;

}
