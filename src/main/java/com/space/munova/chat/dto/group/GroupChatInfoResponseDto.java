package com.space.munova.chat.dto.group;

import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.product.domain.enums.ProductCategory;

import java.time.LocalDateTime;
import java.util.List;

public record GroupChatInfoResponseDto(
        Long chatId,
        String name,
        Integer maxParticipant,
        Integer currentParticipant,
        ChatStatus status,
        LocalDateTime createdAt,
        List<String> productCategoryList
) {
    public static GroupChatInfoResponseDto of(Chat chat, List<ProductCategory> productCategory) {
        List<String> descriptions = (productCategory == null ? List.<ProductCategory>of() : productCategory)
                .stream()
                .filter(c -> c != null) // null 제거
                .map(ProductCategory::getDescription)
                .toList();
        
        return new GroupChatInfoResponseDto(
                chat.getId(), chat.getName(), chat.getMaxParticipant(), chat.getCurParticipant(), chat.getStatus(), chat.getCreatedAt(), descriptions
        );
    }
}
