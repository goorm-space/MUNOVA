package com.space.munova.product.application.product.query.dto;

import com.space.munova.product.domain.enums.OptionCategory;

public record ProductOptionInfoDto (Long optionId,
                                    Long detailId,
                                    OptionCategory optionType,
                                    String optionName,
                                    int quantity){
}
