package com.space.munova.product.application.dto;

import com.space.munova.product.domain.enums.OptionCategory;

public record ProductOptionInfoDto (Long optionId,
                                    Long detailId,
                                    OptionCategory optionType,
                                    String optionName,
                                    int quantity){
}
