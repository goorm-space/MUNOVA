package com.space.munova.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class OneToOneChatRequestDto {

    private Long buyerId;

    private Long sellerId;

    private Long productId;

}
