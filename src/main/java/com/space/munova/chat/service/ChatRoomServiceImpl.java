package com.space.munova.chat.service;

import com.space.munova.chat.dto.ChatItemDto;
import com.space.munova.chat.dto.group.*;
import com.space.munova.chat.dto.onetoone.OneToOneChatResponseDto;
import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.ChatMember;
import com.space.munova.chat.entity.ChatTag;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import com.space.munova.chat.enums.ChatUserType;
import com.space.munova.chat.exception.ChatException;
import com.space.munova.chat.repository.ChatMemberRepository;
import com.space.munova.chat.repository.ChatRepository;
import com.space.munova.chat.repository.ChatRepositoryCustom;
import com.space.munova.chat.repository.ChatTagRepository;
import com.space.munova.member.dto.MemberRole;
import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.domain.Category;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.Repository.CategoryRepository;
import com.space.munova.product.domain.Repository.ProductRepository;
import com.space.munova.product.domain.enums.ProductCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final MemberRepository memberRepository;
    private final ChatRepository chatRepository;
    private final ProductRepository productRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final CategoryRepository categoryRepository;
    private final ChatTagRepository chatTagRepository;
    private final ChatRepositoryCustom chatRepositoryCustom;

    // 1:1 채팅방 생성
    @Override
    @Transactional
    public OneToOneChatResponseDto createOneToOneChatRoom(Long productId, Long buyerId) {

        Member buyer = memberRepository.findById(buyerId)
                .orElseThrow(() -> MemberException.notFoundException("buyerId :" + buyerId));

        // 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ChatException.cannotFindProductException("productId :" + productId));

        // 판매자(상품 등록자, 문의 대상) 조회 -> 꼭 필요할까?
        Member seller = memberRepository.findById(product.getMember().getId())
                .orElseThrow(() -> MemberException.notFoundException("memberId :" + product.getMember().getId()));

        // 판매자와 문의자 동일인일 경우 생성 불가
        if (product.getMember().getId().equals(buyerId)) {
            throw ChatException.notAllowedToCreateChatWithSelf();
        }

        // 채팅방 이미 있는지 확인
        Optional<ChatMember> existingChat = chatMemberRepository.findExistingChatRoom(buyerId, product.getId());

        // 있으면 기존 채팅방 반환
        if (existingChat.isPresent()) {
            return OneToOneChatResponseDto.of(existingChat.get().getChatId(), buyerId, seller.getId());
        }

        // 1:1 채팅방 생성
        Chat chat = chatRepository.save(
                Chat.createChat(generateChatRoomName(product.getName(), buyer.getUsername()), ChatStatus.OPENED, ChatType.ONE_ON_ONE, product,2, 2));

        // 채팅방 참가자(판매자) 등록
        chatMemberRepository.save(ChatMember.createChatMember(chat, seller, ChatUserType.OWNER, seller.getUsername()));
        chatMemberRepository.save(ChatMember.createChatMember(chat, buyer, ChatUserType.MEMBER, buyer.getUsername()));

        return OneToOneChatResponseDto.of(chat, buyerId, seller.getId());
    }

    // 1:1 채팅 목록 조회(구매자, OPENED)
    @Override
    @Transactional(readOnly = true)
    public List<ChatItemDto> getOneToOneChatRoomsByMember(ChatUserType chatUserType, Long memberId) {

        List<Chat> allChats = chatRepository.findByChatTypeAndChatStatus(memberId, ChatType.ONE_ON_ONE, chatUserType, ChatStatus.OPENED);

        return allChats.stream().map(c -> ChatItemDto.of(c.getId(), c.getName(), c.getLastMessageContent(), c.getLastMessageTime())).toList();
    }

    // 1:1 채팅 목록 조회(판매자, 상태 상관 x)
    @Override
    @Transactional(readOnly = true)
    public List<ChatItemDto> getOneToOneChatRoomsBySeller(Long memberId) {

        List<Chat> allChats = chatRepository.findByChatTypeAndChatStatus(memberId, ChatType.ONE_ON_ONE, ChatUserType.OWNER, null);

        return allChats.stream().map(c -> ChatItemDto.of(c.getId(), c.getName(), c.getLastMessageContent(), c.getLastMessageTime())).toList();
    }



    // group 채팅방 생성, 중복 이름 생성 불가
    @Override
    @Transactional
    public GroupChatInfoResponseDto createGroupChatRoom(GroupChatRequestDto requestDto, Long memberId) {

        // 채팅방 생성자 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> MemberException.notFoundException("memberId :" + memberId));

        // 채팅방 이름 중복 확인
        if (chatRepository.existsByName(requestDto.chatName())) {
            throw ChatException.duplicateChatNameException("chatName :" + requestDto.chatName());
        }

        // Group 채팅방 생성
        Chat chat = chatRepository.save(
                Chat.createChat(requestDto.chatName(), ChatStatus.OPENED, ChatType.GROUP, null, 2, requestDto.maxParticipants()));

        List<Category> categoryList = categoryRepository.findAllById(requestDto.productCategoryId());

        for (Category category : categoryList) {
            ChatTag chatTag = ChatTag.createChatTag(chat, category);
            chatTagRepository.save(chatTag);
        }

        chatMemberRepository.save(ChatMember.createChatMember(chat, member, ChatUserType.OWNER, member.getUsername()));
        List<ProductCategory> list = categoryList.stream().map(Category::getCategoryType).toList();

        return GroupChatInfoResponseDto.of(chat, list);
    }

    // 그룹 채팅방 검색
    @Override
    @Transactional(readOnly = true)
    public List<GroupChatDetailResponseDto> searchGroupChatRooms(String keyword, List<Long> tagIds, Boolean isMine, Long memberId) {

        List<Chat> chatRoomLists = chatRepositoryCustom.findByNameAndTags(keyword, tagIds, memberId, isMine);

        return chatRoomLists.stream().map(GroupChatDetailResponseDto::of).toList();
    }

    // 내가 생성한 그룹 채팅방 검색
    @Override
    @Transactional
    public List<GroupChatDetailResponseDto> getMyGroupChatRooms(Long memberId) {

        List<Chat> chatRoomLists = chatRepository.findByChatTypeAndChatStatus(memberId, ChatType.GROUP, ChatUserType.OWNER, null);

        return chatRoomLists.stream()
                .map(GroupChatDetailResponseDto::of)
                .toList();
    }

    // 1:1 채팅방 상태 -> 판매자(SELLER)가 CLOSED로 변경
    @Override
    @Transactional
    public ChatInfoResponseDto setChatRoomClosed(Long chatId, Long memberId, MemberRole role) {

        ChatMember chatMember = chatMemberRepository.findChatMember(chatId, memberId, ChatStatus.OPENED, ChatType.ONE_ON_ONE, ChatUserType.OWNER)
                .orElseThrow(() -> ChatException.unauthorizedParticipantException("chatId :" + chatId));

        // 이미 닫혀있는 경우 예외 던짐
        chatMember.getChatId().oneToOneChatCloseBySeller(role);

        return ChatInfoResponseDto.of(chatMember.getChatId());
    }

    // 그룹 채팅방 정보 변경(OWNER)
    @Override
    @Transactional
    public ChatInfoResponseDto updateGroupChatInfo(Long chatId, GroupChatUpdateRequestDto groupChatUpdateDto, Long memberId) {

        // 채팅방 정보 및 해당 사용자가 해당 방의 생성자인지 확인
        ChatMember chatMember = chatMemberRepository.findChatMember(chatId, memberId, null, ChatType.GROUP, ChatUserType.OWNER)
                .orElseThrow(() -> ChatException.unauthorizedParticipantException("chatId=" + chatId));

        chatMember.getChatId().updateInfo(groupChatUpdateDto);

        return ChatInfoResponseDto.of(chatMember.getChatId());
    }

    // 일반 멤버의 채팅방 나가기
    @Override
    @Transactional
    public void leaveGroupChat(Long chatId, Long memberId) {

        // 해당 채팅방이 유효한지, 해당 채팅방의 참여자인지 조회
        ChatMember chatMember = chatMemberRepository.findChatMember(chatId, memberId, ChatStatus.OPENED, ChatType.GROUP, ChatUserType.MEMBER)
                .orElseThrow(() -> ChatException.unauthorizedParticipantException("chatId=" + chatId));

        chatMember.getChatId().decrementParticipant();
        chatMemberRepository.delete(chatMember);
    }

    // 그룹 채팅방 참여 (채팅방이 OPENED일 때만)
    @Override
    @Transactional
    public void joinGroupChat(Long chatId, Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> MemberException.notFoundException("memberId=" + memberId));

        // OPENED 상태의 GROUP 채팅방 확인
        Chat chat = chatRepository.findChatByIdAndType(chatId, ChatType.GROUP)
                .orElseThrow(() -> ChatException.invalidChatRoomException("chatId=" + chatId));

        // 해당 채팅방에 이미 참여중인지 확인
        if (chatMemberRepository.existsMemberInChat(chatId, memberId, ChatStatus.OPENED)) return;

        // 정원 증가
        chat.incrementParticipant();


        chatMemberRepository.save(ChatMember.createChatMember(chat, member, ChatUserType.MEMBER, member.getUsername()));
    }

    // Group 채팅방 OWNER가 채팅방 CLOSED로 전환
    @Override
    @Transactional
    public void closeGroupChat(Long chatId, Long memberId) {

        // 해당 채팅방이 OPENED 되어 있고, 이에 대한 OWENER 인 경우
        ChatMember chatMember = chatMemberRepository.findChatMember(chatId, memberId, ChatStatus.OPENED, ChatType.GROUP, ChatUserType.OWNER)
                .orElseThrow(() -> ChatException.unauthorizedParticipantException("chatId=" + chatId));

        chatMember.getChatId().updateChatStatus(ChatStatus.CLOSED);
    }

    // OWNER가 채팅방 OPENED로 전환
    @Override
    @Transactional
    public void openGroupChat(Long chatId, Long memberId) {

        // 해당 채팅방이 CLOSED 되어 있고, 이에 대한 OWENER 인 경우
        ChatMember chatMember = chatMemberRepository.findChatMember(chatId, memberId, ChatStatus.CLOSED, ChatType.GROUP, ChatUserType.OWNER)
                .orElseThrow(() -> ChatException.unauthorizedParticipantException("chatId=" + chatId));

        chatMember.getChatId().updateChatStatus(ChatStatus.OPENED);
    }

    // Service
    @Override
    @Transactional
    public GroupChatDetailResponseDto getGroupChatDetail(Long chatId, Long memberId) {
        // 해당 채팅방의 참여자인지 확인
        if(!chatMemberRepository.existsChatMemberAndMemberIdBy(chatId, memberId)){
            throw ChatException.unauthorizedParticipantException("userId=" + memberId);
        }

        Chat chat = chatRepository.findByIdAndType(chatId, ChatType.GROUP)
                .orElseThrow(() -> ChatException.cannotFindChatException("chatId=" + chatId));

        return GroupChatDetailResponseDto.of(chat);
    }

    private String generateChatRoomName(String productName, String userName) {
        return "[" + productName + "] 문의 - " + (userName != null ? userName : "사용자") + "님";
    }
}
