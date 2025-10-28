package com.space.munova.chat.dto.group;

import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.product.domain.enums.ProductCategory;

import java.time.LocalDateTime;
import java.util.List;

public record GroupChatInfoResponseDto(
        Long chatId,
        String name,
        int maxParticipant,
        ChatStatus status,
        LocalDateTime createdAt,
        List<ProductCategory> productCategoryList
) {
    public static GroupChatInfoResponseDto of(Chat chat, List<ProductCategory> productCategory) {
        return new GroupChatInfoResponseDto(
                chat.getId(), chat.getName(), chat.getMaxParticipant(), chat.getStatus(), chat.getCreatedAt(), productCategory
        );
    }
}
