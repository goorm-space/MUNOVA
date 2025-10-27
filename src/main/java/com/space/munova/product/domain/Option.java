package com.space.munova.product.domain;


import com.space.munova.core.entity.BaseEntity;
import com.space.munova.product.domain.enums.OptionCategory;
import com.space.munova.product.infra.converter.OptionConverter;
import jakarta.persistence.*;
import lombok.*;


@Builder
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "product_option")
public class Option extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_option_id")
    private Long id;

    @Convert(converter = OptionConverter.class)
    private OptionCategory optionType;

    private String optionName;

    public static Option createDefaultOption(OptionCategory optionType, String optionName) {

        return Option.builder()
                .optionType(optionType)
                .optionName(optionName)
                .build();
    }


}
