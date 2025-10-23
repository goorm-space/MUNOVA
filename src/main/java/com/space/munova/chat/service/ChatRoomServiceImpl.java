package com.space.munova.chat.service;

import com.space.munova.chat.dto.group.ChatInfoResponseDto;
import com.space.munova.chat.dto.group.GroupChatRequestDto;
import com.space.munova.chat.dto.ChatItemDto;
import com.space.munova.chat.dto.group.GroupChatUpdateRequestDto;
import com.space.munova.chat.dto.onetoone.OneToOneChatResponseDto;
import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.ChatMember;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import com.space.munova.chat.enums.ChatUserType;
import com.space.munova.chat.exception.ChatException;
import com.space.munova.chat.repository.*;
import com.space.munova.member.dto.MemberRole;
import com.space.munova.member.entity.Member;
import com.space.munova.product.domain.product.Jpa.JpaProductRepository;
import com.space.munova.product.domain.product.Product;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Null;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final MemberRepository userRepository;
    private final ChatRepository chatRepository;
    private final JpaProductRepository productRepository;
    private final ChatMemberRepository chatMemberRepository;

    // 1:1 채팅방 생성
    @Override
    @Transactional
    public OneToOneChatResponseDto createOneToOneChatRoom(Long memberId, Long productId) {

        // 구매자 조회
        Member buyer = userRepository.findById(memberId)
                .orElseThrow(() -> ChatException.cannotFindMemberException("buyerId=" + memberId));

        // 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ChatException.cannotFindProductException("productId :" + productId));

//        // 판매자 조회
//        Member seller = userRepository.findById(product.)
//                .orElseThrow(() -> ChatException.cannotFindMemberException("buyerId=" + memberId));

        // 채팅방 이미 있는지 확인
        Optional<ChatMember> existingChat = chatMemberRepository.findByMemberIdAndProductId(buyer, product);

        // 있으면 기존 채팅방 반환
        if(existingChat.isPresent()) {
            return OneToOneChatResponseDto.to(existingChat.get().getChatId(), buyer, buyer);
        }

        // 1:1 채팅방 생성
        Chat chat = chatRepository.save(Chat.builder()
                        .name(generateChatRoomName(product, buyer))
                        .type(ChatType.ONE_ON_ONE)
                        .status(ChatStatus.OPENED)
                        .maxParticipant(2)
                        .curParticipant(2)
//                        .userId(buyer)
                        .build());
        // 채팅방 참가자(판매자) 등록
        chatMemberRepository.save(new ChatMember(chat, buyer, ChatUserType.MEMBER));
        chatMemberRepository.save(new ChatMember(chat, buyer, ChatUserType.OWNER));

        return OneToOneChatResponseDto.to(chat, buyer, buyer);
    }

    // 채팅방 목록 조회
    @Override
    @Transactional
    public List<ChatItemDto> getOneToOneChatRoomsByMember(Long memberId, ChatUserType chatUserType) {
        // 사용자 조회
        Member buyer = userRepository.findById(memberId)
                .orElseThrow(() -> ChatException.cannotFindMemberException("memberId=" + memberId));

        // 사용자 아이디로 Chat 리스트 조회
        return chatMemberRepository.findAllChats(memberId, ChatType.ONE_ON_ONE, chatUserType, ChatStatus.OPENED);
    }

    // group 채팅방 생성
    @Override
    @Transactional
    public ChatInfoResponseDto createGroupChatRoom(GroupChatRequestDto requestDto) {

        // 채팅방 생성자 조회
        Member member = userRepository.findById(requestDto.getMemberId())
                .orElseThrow(() -> ChatException.cannotFindMemberException("sellerId=" + requestDto.getMemberId()));

        // Group 채팅방 생성
        Chat chat = chatRepository.save(Chat.builder()
                        .name(requestDto.getName())
                        .type(ChatType.GROUP)
                        .status(ChatStatus.OPENED)
                        .curParticipant(1)
                        .maxParticipant(requestDto.getMaxParticipants())
                        .build());

        chatMemberRepository.save(new ChatMember(chat, member, ChatUserType.OWNER));

        return ChatInfoResponseDto.to(chat);
    }

    // 그룹 채팅 목록 조회
    @Override
    @Transactional
    public List<ChatItemDto> getGroupChatRooms(Long memberId) {

        // 사용자 조회
        Member buyer = userRepository.findById(memberId)
                .orElseThrow(() -> ChatException.cannotFindMemberException("memberId=" + memberId));

        return chatMemberRepository.findAllGroupChats(memberId, ChatType.GROUP, ChatStatus.OPENED);
    }

    // 1:1 채팅방 상태 -> 판매자(SELLER)가 CLOSED로 변경
    @Override
    @Transactional
    public ChatInfoResponseDto setChatRoomClosed(Long memberId, Long chatId) {
        // 해당 멤버가 실제로 존재하고 판매자인지,
        Member seller = userRepository.findById(memberId)
                .orElseThrow(() -> ChatException.cannotFindMemberException("buyerId=" + memberId));

        if(seller.getRole() != MemberRole.SELLER) {
            throw ChatException.unauthorizedAccessException("sellerId=" + seller.getId());
        }

        ChatMember chatMember = chatMemberRepository.findChatMember(chatId, memberId, ChatStatus.OPENED, ChatType.ONE_ON_ONE, ChatUserType.MEMBER)
                .orElseThrow(() -> ChatException.unauthorizedParticipantException("chatId=" + chatId));

        chatMember.getChatId().updateStatus(ChatStatus.CLOSED);

        return ChatInfoResponseDto.to(chatMember.getChatId());
    }

    // 그룹 채팅방 정보 변경
    @Override
    @Transactional
    public ChatInfoResponseDto updateGroupChatInfo(Long memberId, Long chatId, GroupChatUpdateRequestDto groupChatUpdateDto) {

        // 사용자 조회
        Member member = userRepository.findById(memberId)
                .orElseThrow(() -> ChatException.cannotFindMemberException("buyerId=" + memberId));

        // 채팅방 정보 및 해당 사용자가 해당 방의 생성자인지 확인
        ChatMember chatMember = chatMemberRepository.findChatMember(chatId, memberId, ChatStatus.OPENED, ChatType.GROUP, ChatUserType.OWNER)
                .orElseThrow(() -> ChatException.unauthorizedParticipantException("chatId=" + chatId));

        chatMember.getChatId().updateMaxParticipant(groupChatUpdateDto.getMaxParticipants());
        chatMember.getChatId().updateName(groupChatUpdateDto.getName());

        return ChatInfoResponseDto.to(chatMember.getChatId());
    }

    // 일반 멤버의 채팅방 나가기
    @Override
    @Transactional
    public void leaveGroupChat(Long memberId, Long chatId) {
        Member member = userRepository.findById(memberId)
                .orElseThrow(() -> ChatException.cannotFindMemberException("buyerId=" + memberId));

        ChatMember chatMember = chatMemberRepository.findChatMember(chatId, memberId, ChatStatus.OPENED, ChatType.GROUP, ChatUserType.MEMBER)
                .orElseThrow(() -> ChatException.unauthorizedParticipantException("chatId=" + chatId));

        chatMember.getChatId().decrementParticipant();
        chatMemberRepository.delete(chatMember);
    }

    // 그룹 채팅방 참여
    @Override
    @Transactional
    public void joinGroupChat(Long memberId, Long chatId) {
        Member member = userRepository.findById(memberId)
                .orElseThrow(() -> ChatException.cannotFindMemberException("buyerId=" + memberId));

        // OPENED 상태의 채팅방 확인
        Chat chat = chatRepository.findOpenedChatById(chatId)
                .orElseThrow(() -> ChatException.unauthorizedParticipantException("chatId=" + chatId));

        // 해당 채팅방에 이미 참여중인지 확인
        if(chatMemberRepository.existsBy(chatId, memberId, ChatStatus.OPENED)){
            throw ChatException.alreadyJoinedException("chatId=" + chatId);
        }

        // 정원 증가
        chat.incrementParticipant();

        chatMemberRepository.save(new ChatMember(chat, member, ChatUserType.MEMBER));
    }


    private String generateChatRoomName(Product product, Member otherUser) {
        return "[" + product.getName() + "] 문의 - " + otherUser.getUsername() + "님";
    }
}
