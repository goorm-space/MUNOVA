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
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
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
    public OneToOneChatResponseDto createOneToOneChatRoom(Long productId) {

        // 채팅방 생성자(구매자)
        Long buyerId = JwtHelper.getMemberId();

        Member buyer = memberRepository.findById(buyerId)
                .orElseThrow(() -> MemberException.notFoundException("buyerId :" + buyerId));

        // 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ChatException.cannotFindProductException("productId :" + productId));

        // 판매자(상품 등록자, 문의 대상) 조회 -> 꼭 필요할까?
        Member seller = memberRepository.findById(product.getMember().getId())
                .orElseThrow(() -> MemberException.notFoundException("memberId :" + product.getMember().getId()));
        log.info("Creating chat room for product " + productId + " and buyer " + buyerId);


        // 판매자와 문의자 동일인일 경우 생성 불가
        if (seller.getId().equals(buyerId)) {
            throw ChatException.notAllowedToCreateChatWithSelf();
        }

        // 채팅방 이미 있는지 확인
        Optional<ChatMember> existingChat = chatMemberRepository.findExistingChatRoom(buyerId, product.getId());

        // 있으면 기존 채팅방 반환
        if (existingChat.isPresent()) {
            return OneToOneChatResponseDto.of(existingChat.get().getChatId(), buyerId, seller.getId());
        }

        // 1:1 채팅방 생성
        Chat chat = chatRepository.save(Chat.builder()
                .name(generateChatRoomName(product.getName(), JwtHelper.getMemberName()))
                .type(ChatType.ONE_ON_ONE)
                .status(ChatStatus.OPENED)
                .maxParticipant(2)
                .curParticipant(2)
                .build());

        // 채팅방 참가자(판매자) 등록
        chatMemberRepository.save(new ChatMember(chat, seller, ChatUserType.OWNER, product, seller.getUsername()));
        chatMemberRepository.save(new ChatMember(chat, buyer, ChatUserType.MEMBER, product, buyer.getUsername()));

        return OneToOneChatResponseDto.of(chat, buyerId, seller.getId());
    }

    // 1:1 채팅 목록 조회(판매자, 구매자)
    @Override
    @Transactional(readOnly = true)
    public List<ChatItemDto> getOneToOneChatRoomsByMember(ChatUserType chatUserType) {
        // 사용자 조회
        Long memberId = JwtHelper.getMemberId();

        // 사용자 아이디로 Chat 리스트 조회
        return chatMemberRepository.findAllChats(memberId, ChatType.ONE_ON_ONE, chatUserType, ChatStatus.OPENED);
    }

    // group 채팅방 생성, 중복 이름 생성 불가
    @Override
    @Transactional
    public GroupChatInfoResponseDto createGroupChatRoom(GroupChatRequestDto requestDto) {

        // 채팅방 생성자 조회
        Long memberId = JwtHelper.getMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> MemberException.notFoundException("memberId :" + memberId));

        // 채팅방 이름 중복 확인
        if (chatRepository.existsByName(requestDto.chatName())) {
            throw ChatException.duplicateChatNameException("chatName :" + requestDto.chatName());
        }

        // Group 채팅방 생성
        Chat chat = chatRepository.save(Chat.builder()
                .name(requestDto.chatName())
                .type(ChatType.GROUP)
                .status(ChatStatus.OPENED)
                .curParticipant(1)
                .maxParticipant(requestDto.maxParticipants())
                .build());

        List<Category> categoryList = categoryRepository.findAllById(requestDto.productCategoryId());

        for (Category category : categoryList) {
            ChatTag chatTag = ChatTag.of(chat, category);
            chatTagRepository.save(chatTag);
        }

        chatMemberRepository.save(new ChatMember(chat, member, ChatUserType.OWNER, member.getUsername()));
        List<ProductCategory> list = categoryList.stream().map(Category::getCategoryType).toList();

        return GroupChatInfoResponseDto.of(chat, list);
    }

    // 그룹 채팅방 검색
    @Override
    @Transactional(readOnly = true)
    public List<GroupChatDetailResponseDto> searchGroupChatRooms(String keyword, List<Long> tagIds, Boolean isMine) {

        Long memberId = isMine ? JwtHelper.getMemberId() : null;

        List<Chat> chatRoomLists = chatRepositoryCustom.findByNameAndTags(keyword, tagIds, memberId);

        return chatRoomLists.stream()
                .map(chat -> GroupChatDetailResponseDto.of(
                        chat.getId(),
                        chat.getName(),
                        chat.getMaxParticipant(),
                        chat.getCurParticipant(),
                        chat.getStatus(),
                        chat.getCreatedAt(),
                        chat.getChatTags() != null
                                ? chat.getChatTags().stream()
                                .filter(Objects::nonNull)
                                .map(ct -> ct.getCategoryType() != null ? ct.getCategoryType().getDescription() : null)
                                .filter(Objects::nonNull)
                                .toList()
                                : List.of(), // null이면 빈 리스트
                        chat.getChatMembers() != null
                                ? chat.getChatMembers().stream()
                                .filter(Objects::nonNull)
                                .map(cm -> cm.getMemberId() != null ? MemberInfoDto.of(cm.getMemberId().getId(), cm.getName()) : null)
                                .filter(Objects::nonNull)
                                .toList()
                                : List.of()
                ))
                .toList();
    }


    // 참여 중인 그룹 채팅 목록 조회
    @Override
    @Transactional(readOnly = true)
    public List<ChatItemDto> getGroupChatRooms() {
        return chatMemberRepository.findGroupChats(
                JwtHelper.getMemberId(), ChatType.GROUP, ChatStatus.OPENED);
    }

    // 전체 그룹 채팅방
    @Override
    @Transactional(readOnly = true)
    public List<ChatItemDto> getAllGroupChatRooms() {
        return chatRepository.findAllGroupChats();
    }

    // 1:1 채팅방 상태 -> 판매자(SELLER)가 CLOSED로 변경
    @Override
    @Transactional
    public ChatInfoResponseDto setChatRoomClosed(Long chatId) {

        // 해당 멤버가 실제로 존재하고 판매자인지
        Long sellerId = JwtHelper.getMemberId();
        if (JwtHelper.getMemberRole() != MemberRole.SELLER) {
            throw ChatException.unauthorizedAccessException("sellerId=" + sellerId);
        }

        // OPENED 되어 있는 채팅방 확인
        Chat chat = chatRepository.findByIdAndType(chatId, ChatType.ONE_ON_ONE)
                .orElseThrow(() -> ChatException.cannotFindChatException("chatId=" + chatId));

        // 해당 채팅방 참여 멤버인지 확인
        if (!chatMemberRepository.existsChatMemberAndMemberIdBy(chat.getId(), sellerId)) {
            throw ChatException.unauthorizedParticipantException("sellerId=" + sellerId);
        }

        // 이미 닫혀있는 경우 예외 던짐
        chat.updateChatStatusClosed(ChatStatus.CLOSED);

        return ChatInfoResponseDto.of(chat);
    }

    // 그룹 채팅방 정보 변경
    @Override
    @Transactional
    public ChatInfoResponseDto updateGroupChatInfo(Long chatId, GroupChatUpdateRequestDto groupChatUpdateDto) {

        // 사용자 조회
        Long memberId = JwtHelper.getMemberId();

        // 채팅방 정보 및 해당 사용자가 해당 방의 생성자인지 확인
        ChatMember chatMember = chatMemberRepository.findChatMember(chatId, memberId, ChatStatus.OPENED, ChatType.GROUP, ChatUserType.OWNER)
                .orElseThrow(() -> ChatException.unauthorizedParticipantException("chatId=" + chatId));

        chatMember.getChatId().updateMaxParticipant(groupChatUpdateDto.maxParticipants());
        chatMember.getChatId().updateName(groupChatUpdateDto.name());

        return ChatInfoResponseDto.of(chatMember.getChatId());
    }

    // 일반 멤버의 채팅방 나가기
    @Override
    @Transactional
    public void leaveGroupChat(Long chatId) {
        // 멤버 아이디 조회
        Long memberId = JwtHelper.getMemberId();

        // 해당 채팅방이 유효한지, 해당 채팅방의 참여자인지 조회
        ChatMember chatMember = chatMemberRepository.findChatMember(chatId, memberId, ChatStatus.OPENED, ChatType.GROUP, ChatUserType.MEMBER)
                .orElseThrow(() -> ChatException.unauthorizedParticipantException("chatId=" + chatId));

        chatMember.getChatId().decrementParticipant();
        chatMemberRepository.delete(chatMember);
    }

    // 그룹 채팅방 참여
    @Override
    @Transactional
    public void joinGroupChat(Long chatId) {

        // 참여자 id 확인
        Long memberId = JwtHelper.getMemberId();

        // OPENED 상태의 GROUP 채팅방 확인
        Chat chat = chatRepository.findOpenedGroupChatById(chatId)
                .orElseThrow(() -> ChatException.invalidChatRoomException("chatId=" + chatId));

        // 해당 채팅방에 이미 참여중인지 확인
        if (chatMemberRepository.existsBy(chatId, memberId, ChatStatus.OPENED)) return;

        // 정원 증가
        chat.incrementParticipant();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> MemberException.notFoundException("memberId=" + memberId));

        chatMemberRepository.save(new ChatMember(chat, member, ChatUserType.MEMBER, member.getUsername()));
    }

    // OWNER가 채팅방 CLOSED로 전환
    @Override
    @Transactional
    public void closeGroupChat(Long chatId) {

        Long memberId = JwtHelper.getMemberId();

        // 해당 채팅방이 OPENED 되어 있고, 이에 대한 OWENER 인 경우
        ChatMember chatMember = chatMemberRepository.findChatMember(chatId, memberId, ChatStatus.OPENED, ChatType.GROUP, ChatUserType.OWNER)
                .orElseThrow(() -> ChatException.unauthorizedParticipantException("chatId=" + chatId));

        chatMember.getChatId().updateChatStatusClosed(ChatStatus.CLOSED);
    }

    // Service
    @Override
    public GroupChatDetailResponseDto getGroupChatDetail(Long chatId) {
        Chat chat = chatRepository.findByIdAndType(chatId, ChatType.GROUP)
                .orElseThrow(() -> ChatException.cannotFindChatException("chatId=" + chatId));

        List<String> productCategoryList = chat.getChatTags().stream()
                .map(ChatTag::getCategoryType)   // ProductCategory
                .filter(pc -> pc != null)
                .map(ProductCategory::getDescription)
                .toList();

        List<MemberInfoDto> memberList = chat.getChatMembers().stream()
                .map(cm -> MemberInfoDto.of(cm.getMemberId().getId(), cm.getName()))
                .toList();

        return new GroupChatDetailResponseDto(
                chat.getId(),
                chat.getName(),
                chat.getMaxParticipant(),
                chat.getCurParticipant(),
                chat.getStatus(),
                chat.getCreatedAt(),
                productCategoryList,
                memberList
        );
    }

    private String generateChatRoomName(String productName, String userName) {
        return "[" + productName + "] 문의 - " + userName + "님";
    }
}
