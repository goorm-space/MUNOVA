package com.space.munova.order.dto;

import com.space.munova.order.entity.OrderItem;
import com.space.munova.product.domain.ProductImage;
import com.space.munova.product.domain.enums.ProductImageType;
import lombok.Builder;

import java.util.stream.Collectors;

@Builder
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
                orderItem.getPriceSnapshot()*orderItem.getQuantity(),
                orderItem.getStatus(),
                orderItem.getProductDetail().getProduct().getProductImages().stream().filter(image -> image.getImageType() == ProductImageType.MAIN).map(ProductImage::getImgUrl).findFirst().orElse(null)
        );
    }
}
