package com.space.munova.order.dto;

import com.space.munova.order.entity.OrderItem;

import java.util.stream.Collectors;

public record OrderItemDto(
        Long orderItemId,
        String brandName,
        String productName,
        String option,
        Integer quantity,
        Long totalPrice,
        OrderStatus status,
        String imageUrl
) {
    public static OrderItemDto from(OrderItem orderItem) {
        String optionStr = OptionDto.combineOptionNamesByType(
                orderItem.getProductDetail()
                        .getOptionMappings()
                        .stream()
                        .map(mapping -> new OptionDto(
                                mapping.getOption().getId(),
                                mapping.getOption().getOptionType(),
                                mapping.getOption().getOptionName()
                        ))
                        .collect(Collectors.toList())
        );

        return new OrderItemDto(
                orderItem.getId(),
                orderItem.getProductDetail().getProduct().getBrand().getBrandName(),
                orderItem.getNameSnapshot(),
                optionStr,
                orderItem.getQuantity(),
                orderItem.getPriceSnapshot(),
                orderItem.getStatus(),
"https://encrypted-tbn3.gstatic.com/shopping?q=tbn:ANd9GcREdixMliFliUNy1jx3BOcvILQ-NYjVbkF63dVbnTFLybgPvHMHqSaUP5M7ur6G--T5KngDwZVTZt3w4DcFUeb-crAWVWic2h92FAIlg8DHvuOLTvOrKaEQdzH_cwlqpG3nqoU20VE&usqp=CAc"
//                orderItem.getProductDetail().getProduct().getProductImages().getFirst().getOriginName()
        );
    }
}
