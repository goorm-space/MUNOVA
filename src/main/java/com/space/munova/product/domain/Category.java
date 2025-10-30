package com.space.munova.product.domain;


import com.space.munova.core.entity.BaseEntity;
import com.space.munova.product.domain.enums.ProductCategory;
import com.space.munova.product.infra.converter.ProductCategoryConverter;
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
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_category_id")
    private Long id;

    @Nullable
    @ManyToOne
    @JoinColumn(name = "ref_product_category_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Category refCategory;

    @Convert(converter = ProductCategoryConverter.class)
    private ProductCategory categoryType;

    private String categoryName;

    private Integer level;

}
