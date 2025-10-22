package com.space.munova.chat.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class GroupChatRequestDto {

    private Long memberId;  // 채팅방 생성자 아이디

//    private ChatUserType chatUserType;  // 채팅방 참여자 권한 -> 생성자는 권한 굳이 안받아도 되지 뭐 참여할때나 그렇지

    @NotBlank(message = "채팅방 이름은 필수입니다.")
    private String name;    // 채팅방 이름

    @NotNull(message = "최대 참여 인원 설정은 필수입니다.")
    private int maxParticipants;    // 최대 참여 인원 제한

}
