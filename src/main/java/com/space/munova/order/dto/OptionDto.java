package com.space.munova.order.dto;

import com.space.munova.product.domain.Option;
import com.space.munova.product.domain.enums.OptionCategory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public record OptionDto (
        Long optionId,
        OptionCategory optionType,
        String optionName
){
    public static OptionDto from(Option option){
        return new OptionDto(
                option.getId(),
                option.getOptionType(),
                option.getOptionName()
        );
    }

    public static String combineOptionNamesByType(List<OptionDto> options) {
        if (options == null || options.isEmpty()) {
            return "옵션 없음";
        }

        // 우선순위를 정해줍니다 (COLOR → SIZE 순)
        List<String> priority = List.of("COLOR", "SIZE");

        // optionType 순으로 정렬
        List<OptionDto> sortedOptions = options.stream()
                .sorted(Comparator.comparingInt(o -> {
                    int index = priority.indexOf(o.optionType());
                    return index == -1 ? Integer.MAX_VALUE : index;
                }))
                .collect(Collectors.toList());

        // optionName만 추출하여 공백으로 합침
        return sortedOptions.stream()
                .map(OptionDto::optionName)
                .collect(Collectors.joining(" "));
    }
}
