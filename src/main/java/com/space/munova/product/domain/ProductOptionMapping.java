package com.space.munova.product.domain;


import com.space.munova.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "product_option_mapping")
public class ProductOptionMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_option_mapping_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "option_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Option option;

    @ManyToOne
    @JoinColumn(name = "product_detail_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ProductDetail productDetail;

    @ColumnDefault("0")
    private boolean isDeleted;

    public static ProductOptionMapping createDefaultProductOptionMapping(Option option, ProductDetail productDetail) {

        return ProductOptionMapping.builder()
                .option(option)
                .productDetail(productDetail)
                .build();
    }

}
