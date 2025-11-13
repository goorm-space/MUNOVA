package com.space.munova.chat.unit.service;

import com.space.munova.chat.dto.ChatItemDto;
import com.space.munova.chat.dto.group.ChatInfoResponseDto;
import com.space.munova.chat.dto.group.GroupChatDetailResponseDto;
import com.space.munova.chat.dto.group.GroupChatRequestDto;
import com.space.munova.chat.dto.group.GroupChatUpdateRequestDto;
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
import com.space.munova.chat.service.ChatRoomServiceImpl;
import com.space.munova.member.dto.MemberRole;
import com.space.munova.member.entity.Member;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.domain.Brand;
import com.space.munova.product.domain.Category;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.Repository.CategoryRepository;
import com.space.munova.product.domain.Repository.ProductRepository;
import com.space.munova.product.domain.enums.ProductCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatRoomServiceTest {

    @InjectMocks
    private ChatRoomServiceImpl chatRoomService;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ChatRepository chatRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ChatMemberRepository chatMemberRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ChatTagRepository chatTagRepository;
    @Mock
    private ChatRepositoryCustom chatRepositoryCustom;

    private Member buyer;
    private Member seller;
    private Product product;
    private Chat chat;
    private Chat groupChat;
    private Brand brand;
    private Category category1;
    private Category category2;
    private Category category3;
    private ChatMember chatMemberBuyer;
    private ChatMember chatMemberSeller;
    private ChatMember chatMemberGroup;

    // Helper Method
    private Member createMember(Long id, String userName){
        Member m = Member.createMember(userName, "0000", "SEOUL");
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private Category createCategory(Long id, ProductCategory type) {
        return new Category(id, null, type, type.getDescription(), 1);
    }

    private Product createProduct(Long id, String name, Member seller) {
        Brand brand = new Brand(1L, "nike", "1", "nike");

        Category category = createCategory(1L, ProductCategory.A_WINTER_BOOTS);

        Product product = Product.createDefaultProduct(name, "product information must be at least 10 characters long", 10000L, brand, category, seller);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private Chat createChat(Long id, String name, ChatType type, Product product, int max) {
        Chat chat = Chat.createChat(name, ChatStatus.OPENED, type, product, 2, max);
        ReflectionTestUtils.setField(chat, "id", id);
        return chat;
    }

    private ChatMember createChatMember(Long id, Chat chat, Member member, ChatUserType type) {
        ChatMember cm = ChatMember.createChatMember(chat, member, type, member.getUsername());
        ReflectionTestUtils.setField(cm, "id", id);
        chat.getChatMembers().add(cm);
        return cm;
    }

    private Chat createGroupChatWithTags(Long id, String chatName, Member owner, List<Category> categories) {
        Chat chat = createChat(id, chatName, ChatType.GROUP, null, 10);
        ReflectionTestUtils.setField(chat, "id", id);

        for (Category c : categories) {
            chat.getChatTags().add(ChatTag.createChatTag(chat, c));
        }

        createChatMember(3L, chat, owner, ChatUserType.OWNER);
        return chat;
    }

    private Chat createOneToOneChat(Long id, String chatName, Product product, Member seller, Member buyer) {
        Chat chat = createChat(id, chatName, ChatType.ONE_ON_ONE, product, 2);
        createChatMember(1L, chat, buyer, ChatUserType.MEMBER);
        createChatMember(2L, chat, seller, ChatUserType.OWNER);
        return chat;
    }



    @Nested
    @DisplayName("상품 ID와 구매자 ID로 1:1 채팅방을 생성할 수 있다.")
    class OneToOneChatCreateTest {
        @Test
        @DisplayName("1:1 채팅방 생성 - 해피케이스")
        void createOneToOneChatRoom() {
            // setup
            Member buyer = createMember(1L, "buyer");
            Member seller = createMember(2L, "seller");
            Product product = createProduct(10L, "product", seller);
            Chat chat = createOneToOneChat(99L, "chatName", product, buyer, seller);

            // given
            given(memberRepository.findById(anyLong()))
                    .willAnswer(inv -> {
                        Long id = inv.getArgument(0);
                        if (id.equals(1L)) return Optional.of(buyer);   // buyer
                        if (id.equals(2L)) return Optional.of(seller);  // seller
                        return Optional.empty();
                    });
            given(productRepository.findById(10L)).willReturn(Optional.of(product));
            given(chatMemberRepository.findExistingChatRoom(1L, 10L)).willReturn(Optional.empty());
            given(chatRepository.save(any(Chat.class))).willReturn(chat);

            // when
            OneToOneChatResponseDto result = chatRoomService.createOneToOneChatRoom(10L, 1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.chatId()).isEqualTo(99L);
            assertThat(result.buyerId()).isEqualTo(1L);
            assertThat(result.sellerId()).isEqualTo(2L);

            verify(memberRepository, times(2)).findById(anyLong());
            verify(productRepository, times(1)).findById(10L);
            verify(chatRepository, times(1)).save(any(Chat.class));
            verify(chatMemberRepository, times(1)).findExistingChatRoom(1L, 10L);
            verify(chatMemberRepository, times(2)).save(any());
        }


        @Test
        @DisplayName("이미 존재하는 1:1 채팅방이 있으면 새로 만들지 않는다.")
        void shouldReturnExistingChatRoomWhenAlreadyExists() {
            // setup
            Member buyer = createMember(1L, "buyer");
            Member seller = createMember(2L, "seller");
            Product product = createProduct(10L, "product", seller);

            Chat existingChat = createOneToOneChat(99L, "existingChat", product, seller, buyer);
            ChatMember existingChatMember = existingChat.getChatMembers().getFirst(); // buyer 기준 member

            // given
            given(memberRepository.findById(anyLong())).willAnswer(inv -> {
                Long id = inv.getArgument(0);
                if (id.equals(1L)) return Optional.of(buyer);
                if (id.equals(2L)) return Optional.of(seller);
                return Optional.empty();
            });
            given(productRepository.findById(10L)).willReturn(Optional.of(product));
            given(chatMemberRepository.findExistingChatRoom(1L, 10L))
                    .willReturn(Optional.of(existingChatMember)
            );

            // when
            OneToOneChatResponseDto result = chatRoomService.createOneToOneChatRoom(10L, 1L);

            // then
            assertThat(result.chatId()).isEqualTo(99L);
            assertThat(result.name()).isEqualTo("existingChat");

            verify(memberRepository, times(2)).findById(anyLong());
            verify(productRepository, times(1)).findById(10L);
            verify(chatRepository, times(0)).save(any(Chat.class));
            verify(chatMemberRepository, times(1)).findExistingChatRoom(1L, 10L);
            verify(chatMemberRepository, times(0)).save(any());
        }

        @Test
        @DisplayName("구매자와 판매자가 동일하면 예외가 발생한다.")
        void shouldThrowExceptionWhenBuyerEqualsSeller() {
            // setup
            Member buyer = createMember(1L, "buyer");
            Product product = createProduct(10L, "product", buyer);

            // given
            given(memberRepository.findById(anyLong()))
                    .willAnswer(inv -> {
                        Long id = inv.getArgument(0);
                        if (id.equals(1L)) return Optional.of(buyer);  // buyer
                        if (id.equals(2L)) return Optional.of(seller); // seller
                        return Optional.empty();
                    });

            given(productRepository.findById(10L)).willReturn(Optional.of(product));

            // when
            assertThatThrownBy(() -> chatRoomService.createOneToOneChatRoom(10L, 1L))
                    .isInstanceOf(ChatException.class)
                    .hasMessageContaining("자신이 등록한 상품에는 문의할 수 없습니다.");

            // then
            verify(memberRepository, times(2)).findById(anyLong());
            verify(productRepository, times(1)).findById(10L);
            verify(chatRepository, times(0)).save(any(Chat.class));
            verify(chatMemberRepository, times(0)).findExistingChatRoom(1L, 10L);
            verify(chatMemberRepository, times(0)).save(any());
        }
    }

    @Nested
    @DisplayName("1:1 채팅방 목록 조회(판매자, 구매자)")
    class GetOneToOneChatRoomsByMember {
        @Test
        @DisplayName("1:1 채팅방 목록 조회 (buyer)")
        void getOneToOneChatRoomsByBuyer() {
            // setup
            Member buyer = createMember(1L, "buyer");
            Member seller = createMember(2L, "seller");
            Product product = createProduct(10L, "product", seller);
            Chat chat = createOneToOneChat(99L, "chatName", product, buyer, seller);

            // given
            given(chatRepository.findByChatTypeAndChatStatus(
                    buyer.getId(), ChatType.ONE_ON_ONE, ChatUserType.MEMBER, ChatStatus.OPENED)
            ).willReturn(List.of(chat));
            chat.modifyLastMessageContent("Hello", LocalDateTime.now());

            // when
            List<ChatItemDto> result = chatRoomService.getOneToOneChatRoomsByMember(ChatUserType.MEMBER, buyer.getId());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().chatId()).isEqualTo(99L);
            assertThat(result.getFirst().name()).isEqualTo("chatName");
            assertThat(result.getFirst().lastMessageContent()).isEqualTo("Hello");

            // verify repository call
            verify(chatRepository, times(1))
                    .findByChatTypeAndChatStatus(buyer.getId(), ChatType.ONE_ON_ONE, ChatUserType.MEMBER, ChatStatus.OPENED);
        }

        @Test
        @DisplayName("1:1 채팅방 목록 조회 (seller)")
        void getOneToOneChatRoomsBySeller() {
            // setup
            Member buyer = createMember(1L, "buyer");
            Member seller = createMember(2L, "seller");
            Product product = createProduct(10L, "product", seller);
            Chat chat = createOneToOneChat(99L, "chatName", product, buyer, seller);

            // given
            given(chatRepository.findByChatTypeAndChatStatus(
                    seller.getId(), ChatType.ONE_ON_ONE, ChatUserType.OWNER, ChatStatus.OPENED)
            ).willReturn(List.of(chat));
            chat.modifyLastMessageContent("Hello", LocalDateTime.now());

            // when
            List<ChatItemDto> result = chatRoomService.getOneToOneChatRoomsByMember(ChatUserType.OWNER, seller.getId());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().chatId()).isEqualTo(99L);
            assertThat(result.getFirst().name()).isEqualTo("chatName");
            assertThat(result.getFirst().lastMessageContent()).isEqualTo("Hello");

            // verify repository call
            verify(chatRepository, times(1))
                    .findByChatTypeAndChatStatus(seller.getId(), ChatType.ONE_ON_ONE, ChatUserType.OWNER, ChatStatus.OPENED);
        }
    }


    @Nested
    @DisplayName("그룹 채팅방 생성 테스트")
    class CreateGroupChatRoomTest {

        @Test
        @DisplayName("그룹 채팅방 생성 - 해피케이스 (태그 없음)")
        void createGroupChatRoom_success() {
            // setup
            Member member = createMember(1L, "member");
            Category category1 = createCategory(1L, ProductCategory.A_BOOTS);
            Category category2 = createCategory(2L, ProductCategory.M_LOAFERS);
            Category category3 = createCategory(3L, ProductCategory.M_SLIPPERS);
            Chat groupChat = createGroupChatWithTags(777L, "GroupRoomNoTags", member, List.of());

            // 요청 DTO
            GroupChatRequestDto requestDto = new GroupChatRequestDto(
                    "GroupRoomNoTags",
                    10,
                    List.of()   // 태그 없음
            );

            // geiven
            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
            given(chatRepository.existsByName("GroupRoomNoTags")).willReturn(false);
            given(chatRepository.save(any(Chat.class))).willReturn(groupChat);
            given(categoryRepository.findAllById(List.of())).willReturn(List.of());

            // when
            var result = chatRoomService.createGroupChatRoom(requestDto, member.getId());

            // then
            assertThat(result).isNotNull();
            assertThat(result.chatId()).isEqualTo(777L);
            assertThat(result.name()).isEqualTo("GroupRoomNoTags");
            assertThat(result.productCategoryList()).isEmpty();

            // verify
            verify(memberRepository, times(1)).findById(member.getId());
            verify(chatRepository, times(1)).existsByName("GroupRoomNoTags");
            verify(chatRepository, times(1)).save(any(Chat.class));
            verify(categoryRepository, times(1)).findAllById(List.of());
            verify(chatTagRepository, times(0)).save(any()); // 태그 없음
            verify(chatMemberRepository, times(1)).save(any());
        }


        @Test
        @DisplayName("그룹 채팅방 생성 - 해피케이스 (카테고리 태그 있음)")
        void createGroupChatRoom_withTags_success() {
            // setup
            Member member = createMember(1L, "member");
            Category category1 = createCategory(1L, ProductCategory.A_BOOTS);
            Category category2 = createCategory(2L, ProductCategory.M_LOAFERS);
            Category category3 = createCategory(3L, ProductCategory.M_SLIPPERS);
            List<Category> categories = List.of(category1, category2, category3);
            Chat groupChat = createGroupChatWithTags(888L, "GroupRoomWithTags", member, categories);

            // given
            GroupChatRequestDto requestDto = new GroupChatRequestDto(
                    "GroupRoomWithTags",
                    5,
                    List.of(1L, 2L, 3L)
            );

            List<Category> categoryList = List.of(category1, category2, category3);

            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
            given(chatRepository.existsByName("GroupRoomWithTags")).willReturn(false);
            given(chatRepository.save(any(Chat.class))).willReturn(groupChat);
            given(categoryRepository.findAllById(requestDto.productCategoryId())).willReturn(categoryList);

            // when
            var result = chatRoomService.createGroupChatRoom(requestDto, member.getId());

            // then
            assertThat(result).isNotNull();
            assertThat(result.chatId()).isEqualTo(888L);
            assertThat(result.name()).isEqualTo("GroupRoomWithTags");
            assertThat(result.productCategoryList())
                    .containsExactlyInAnyOrder("부츠", "슬리퍼", "로퍼"); // ✅ description 기준으로 비교

            // verify
            verify(memberRepository, times(1)).findById(member.getId());
            verify(chatRepository, times(1)).existsByName("GroupRoomWithTags");
            verify(chatRepository, times(1)).save(any(Chat.class));
            verify(categoryRepository, times(1)).findAllById(requestDto.productCategoryId());
            verify(chatTagRepository, times(3)).save(any());
            verify(chatMemberRepository, times(1)).save(any());
        }


        @Test
        @DisplayName("중복된 이름으로 생성 시 예외 발생")
        void createGroupChatRoom_duplicateName_throwsException() {
            // setup
            Member member = createMember(1L, "member");

            // given
            GroupChatRequestDto requestDto = new GroupChatRequestDto(
                    "GroupRoomNoTags",
                    10,
                    List.of()   // 태그 없음
            );

            // 이미 존재하는 채팅방이 있다고 가정
            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
            given(chatRepository.existsByName("GroupRoomNoTags")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> chatRoomService.createGroupChatRoom(requestDto, member.getId()))
                    .isInstanceOf(ChatException.class)
                    .hasMessageContaining("이미 존재하는 채팅방 이름입니다.");

            // verify — 저장은 절대 일어나면 안 됨
            verify(memberRepository, times(1)).findById(member.getId());
            verify(chatRepository, times(1)).existsByName("GroupRoomNoTags");
            verify(chatRepository, times(0)).save(any(Chat.class));
            verify(categoryRepository, times(0)).findAllById(anyList());
            verify(chatTagRepository, times(0)).save(any());
            verify(chatMemberRepository, times(0)).save(any());
        }
    }

    @Nested
    @DisplayName("그룹 채팅방 검색 테스트")
    class SearchGroupChatRoomsTest {

        @Test
        @DisplayName("그룹 채팅방 검색 - 키워드, 태그, isMine 조합 (해피케이스)")
        void searchGroupChatRooms_success() {
            // setup
            Member member1 = createMember(1L, "member1");

            Category category1 = createCategory(1L, ProductCategory.A_BOOTS);
            Category category2 = createCategory(2L, ProductCategory.M_LOAFERS);
            Category category3 = createCategory(3L, ProductCategory.M_SLIPPERS);

            List<Category> categories1 = List.of(category1, category2);
            List<Category> categories2 = List.of(category1, category3);

            Chat groupChat1 = createGroupChatWithTags(888L, "운동화 관련 질문", member1, categories1);
            Chat groupChat2 = createGroupChatWithTags(999L, "슬리퍼 관련 질문", member1, categories2);

            // given
            given(chatRepositoryCustom.findByNameAndTags("운동화", List.of(1L, 2L), member1.getId(), true))
                    .willReturn(List.of(groupChat1, groupChat2));

            // when
            List<GroupChatDetailResponseDto> result =
                    chatRoomService.searchGroupChatRooms("운동화", List.of(1L, 2L), true, member1.getId());

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);

            // 모든 채팅방에 대해 공통 검증
            for (GroupChatDetailResponseDto dto : result) {
                // 이름 확인
                assertThat(dto.name()).isIn("운동화 관련 질문", "슬리퍼 관련 질문");

                // 태그 리스트 확인
                assertThat(dto.productCategoryList())
                        .isNotEmpty()
                        .allSatisfy(tag ->
                                assertThat(tag).isIn("부츠", "슬리퍼", "로퍼")
                        );

                // 멤버 리스트 확인
                assertThat(dto.memberList())
                        .extracting("name")
                        .contains("member1");
            }

            // verify
            verify(chatRepositoryCustom, times(1))
                    .findByNameAndTags("운동화", List.of(1L, 2L), member1.getId(), true);
        }

        @Test
        @DisplayName("isMine=true -> 내가 참여한 그룹방만 조회")
        void searchGroupChatRooms_isMine_true() {
            // setup
            Member member1 = createMember(1L, "member1");
            Member member2 = createMember(2L, "member2");

            Category category1 = createCategory(1L, ProductCategory.A_BOOTS);
            Category category2 = createCategory(2L, ProductCategory.M_LOAFERS);
            Category category3 = createCategory(3L, ProductCategory.M_SLIPPERS);

            List<Category> categories1 = List.of(category1, category2);
            List<Category> categories2 = List.of(category1, category3);

            Chat groupChat1 = createGroupChatWithTags(888L, "운동화 관련 질문", member1, categories1);
            Chat groupChat2 = createGroupChatWithTags(999L, "슬리퍼 관련 질문", member2, categories2);

            // given
            given(chatRepositoryCustom.findByNameAndTags("관련", List.of(1L), member1.getId(), true))
                    .willReturn(List.of(groupChat1));

            // when
            List<GroupChatDetailResponseDto> result =
                    chatRoomService.searchGroupChatRooms("관련", List.of(1L), true, member1.getId());

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            GroupChatDetailResponseDto dto = result.getFirst();
            assertThat(dto.name()).isEqualTo("운동화 관련 질문");
            assertThat(dto.memberList()).extracting("name").contains("member1");
            assertThat(dto.chatId()).isEqualTo(groupChat1.getId());

            // verify
            verify(chatRepositoryCustom, times(1))
                    .findByNameAndTags("관련", List.of(1L), member1.getId(), true);
        }

        @Test
        @DisplayName("isMine=false -> 전체 그룹방 조회 (내가 참여하지 않아도 조회 가능)")
        void searchGroupChatRooms_isMine_false() {
            // setup
            Member member1 = createMember(1L, "member1");
            Member member2 = createMember(2L, "member2");

            Category category1 = createCategory(1L, ProductCategory.A_BOOTS);
            Category category2 = createCategory(2L, ProductCategory.M_LOAFERS);
            Category category3 = createCategory(3L, ProductCategory.M_SLIPPERS);

            List<Category> categories1 = List.of(category1, category2);
            List<Category> categories2 = List.of(category1, category3);

            Chat groupChat1 = createGroupChatWithTags(888L, "운동화 관련 질문", member2, categories1);
            Chat groupChat2 = createGroupChatWithTags(999L, "슬리퍼 관련 질문", member2, categories2);

            given(chatRepositoryCustom.findByNameAndTags("관련", List.of(1L), member1.getId(), false))
                    .willReturn(List.of(groupChat1,  groupChat2));

            // when
            List<GroupChatDetailResponseDto> result =
                    chatRoomService.searchGroupChatRooms("관련", List.of(1L), false, member1.getId());

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);

            for (GroupChatDetailResponseDto dto : result) {
                assertThat(dto.name()).isIn("운동화 관련 질문", "슬리퍼 관련 질문");
                assertThat(dto.productCategoryList())
                        .isNotEmpty()
                        .allSatisfy(tag ->
                                assertThat(tag).isIn("부츠", "슬리퍼", "로퍼")
                        );
                assertThat(dto.memberList())
                        .extracting("name")
                        .contains("member2");
            }
            // verify
            verify(chatRepositoryCustom, times(1))
                    .findByNameAndTags("관련", List.of(1L), member1.getId(), false);
        }

    }

    @Nested
    @DisplayName("내가 생성한 그룹 채팅방 조회 테스트")
    class GetMyGroupChatRoomsTest {

        @Test
        @DisplayName("해피케이스 - OWNER로 참여한 그룹 채팅방만 조회된다.")
        void getMyGroupChatRooms_success() {
            // given
            Member member1 = createMember(1L, "member1");
            Member member2 = createMember(2L, "member2");

            Category category1 = createCategory(1L, ProductCategory.A_BOOTS);
            Category category2 = createCategory(2L, ProductCategory.M_LOAFERS);
            Category category3 = createCategory(3L, ProductCategory.M_SLIPPERS);

            List<Category> categories1 = List.of(category1, category2);
            List<Category> categories2 = List.of(category1, category3);

            Chat groupChat1 = createGroupChatWithTags(888L, "운동화 관련 질문", member1, categories1);
            Chat groupChat2 = createGroupChatWithTags(999L, "슬리퍼 관련 질문", member2, categories2);

            given(chatRepository.findByChatTypeAndChatStatus(member1.getId(), ChatType.GROUP, ChatUserType.OWNER, null))
                    .willReturn(List.of(groupChat1));

            // when
            List<GroupChatDetailResponseDto> result = chatRoomService.getMyGroupChatRooms(member1.getId());

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result)
                    .extracting(GroupChatDetailResponseDto::name)
                    .containsExactlyInAnyOrder("운동화 관련 질문");

            assertThat(result.getFirst().memberList())
                    .extracting("name")
                    .contains("member1");

            // verify
            verify(chatRepository, times(1))
                    .findByChatTypeAndChatStatus(member1.getId(), ChatType.GROUP, ChatUserType.OWNER, null);
        }

        @Test
        @DisplayName("내가 생성한 그룹 채팅방이 하나도 없을 경우 빈 리스트 반환")
        void getMyGroupChatRooms_empty() {
            // given
            Member member1 = createMember(1L, "member1");
            Member member2 = createMember(2L, "member2");

            Category category1 = createCategory(1L, ProductCategory.A_BOOTS);
            Category category2 = createCategory(2L, ProductCategory.M_LOAFERS);
            Category category3 = createCategory(3L, ProductCategory.M_SLIPPERS);

            List<Category> categories1 = List.of(category1, category2);
            List<Category> categories2 = List.of(category1, category3);

            Chat groupChat1 = createGroupChatWithTags(888L, "운동화 관련 질문", member2, categories1);
            Chat groupChat2 = createGroupChatWithTags(999L, "슬리퍼 관련 질문", member2, categories2);

            // given
            given(chatRepository.findByChatTypeAndChatStatus(member1.getId(), ChatType.GROUP, ChatUserType.OWNER, null))
                    .willReturn(List.of());

            // when
            List<GroupChatDetailResponseDto> result = chatRoomService.getMyGroupChatRooms(member1.getId());

            // then
            assertThat(result).isEmpty();

            // verify
            verify(chatRepository, times(1))
                    .findByChatTypeAndChatStatus(member1.getId(), ChatType.GROUP, ChatUserType.OWNER, null);
        }
    }


    @Nested
    @DisplayName("1:1 채팅방 종료(setChatRoomClosed) 테스트")
    class SetChatRoomClosedTest {

        @Test
        @DisplayName("판매자가 OPENED 상태의 1:1 채팅방을 닫을 수 있다 (해피케이스)")
        void setChatRoomClosed_success() {
            // setup
            Member seller = createMember(2L, "seller");
            Product product = createProduct(10L, "product", seller);
            Chat chat = createChat(99L, "chatName", ChatType.ONE_ON_ONE, product, 2);
            ChatMember chatMemberSeller = createChatMember(2L, chat, seller, ChatUserType.OWNER);

            // given
            given(chatMemberRepository.findChatMember(
                    99L, seller.getId(), ChatStatus.OPENED, ChatType.ONE_ON_ONE, ChatUserType.OWNER
            )).willReturn(Optional.of(chatMemberSeller));

            // when
            ChatInfoResponseDto result = chatRoomService.setChatRoomClosed(99L, seller.getId(), MemberRole.SELLER);

            // then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(ChatStatus.CLOSED);

            // verify
            verify(chatMemberRepository, times(1))
                    .findChatMember(99L, seller.getId(), ChatStatus.OPENED, ChatType.ONE_ON_ONE, ChatUserType.OWNER);
        }

        @Test
        @DisplayName("비판매자(일반 사용자)는 채팅방을 닫을 수 없다")
        void setChatRoomClosed_unauthorizedRole_throwsException() {
            // setup
            Member buyer = createMember(1L, "buyer");
            Member seller = createMember(2L, "seller");
            Product product = createProduct(10L, "product", seller);

            // given — MEMBER는 OWNER가 아니므로 조회 결과 없음
            given(chatMemberRepository.findChatMember(
                    99L, buyer.getId(), ChatStatus.OPENED, ChatType.ONE_ON_ONE, ChatUserType.OWNER
            )).willReturn(Optional.empty());

            // when
            assertThatThrownBy(() ->
                    chatRoomService.setChatRoomClosed(99L, buyer.getId(), MemberRole.USER)
            )
                    .isInstanceOf(ChatException.class)
                    .hasMessageContaining("채팅방에 대한 권한이 없습니다."); // unauthorizedParticipantException 메시지
            // verify
            verify(chatMemberRepository, times(1))
                    .findChatMember(99L, buyer.getId(), ChatStatus.OPENED, ChatType.ONE_ON_ONE, ChatUserType.OWNER);
        }

        @Test
        @DisplayName("참여자가 아닌 경우 예외 발생")
        void setChatRoomClosed_notParticipant_throwsException() {
            // setup
            Member otherMember = createMember(1L, "otherMember");

            // given
            given(chatMemberRepository.findChatMember(
                    99L, otherMember.getId(), ChatStatus.OPENED, ChatType.ONE_ON_ONE, ChatUserType.OWNER
            )).willReturn(Optional.empty());

            // when
            assertThatThrownBy(() ->
                    chatRoomService.setChatRoomClosed(99L, otherMember.getId(), MemberRole.SELLER)
            )
                    .isInstanceOf(ChatException.class)
                    .hasMessageContaining("채팅방에 대한 권한이 없습니다.");

            // verify
            verify(chatMemberRepository, times(1))
                    .findChatMember(99L, otherMember.getId(), ChatStatus.OPENED, ChatType.ONE_ON_ONE, ChatUserType.OWNER);
        }

        @Test
        @DisplayName("이미 닫혀있는 채팅방은 다시 닫을 수 없다")
        void setChatRoomClosed_alreadyClosed_throwsException() {
            // setup
            Member seller = createMember(1L, "seller");
            Product product = createProduct(10L, "product", seller);
            Chat chat = createChat(99L, "chatName", ChatType.ONE_ON_ONE, product, 2);
            ReflectionTestUtils.setField(chat, "status", ChatStatus.CLOSED);

            ChatMember chatMemberSeller = createChatMember(1L, chat, seller, ChatUserType.OWNER);

            // given
            given(chatMemberRepository.findChatMember(
                    99L, seller.getId(), ChatStatus.OPENED, ChatType.ONE_ON_ONE, ChatUserType.OWNER
            )).willReturn(Optional.of(chatMemberSeller));

            // when
            assertThatThrownBy(() ->
                    chatRoomService.setChatRoomClosed(99L, seller.getId(), MemberRole.SELLER)
            )
                    .isInstanceOf(ChatException.class)
                    .hasMessageContaining("이미 종료된 채팅방입니다.");

            // verify
            verify(chatMemberRepository, times(1))
                    .findChatMember(99L, seller.getId(), ChatStatus.OPENED, ChatType.ONE_ON_ONE, ChatUserType.OWNER);
        }
    }

    @Nested
    @DisplayName("그룹 채팅방 정보 수정(updateGroupChatInfo) 테스트")
    class UpdateGroupChatInfoTest {

        @Test
        @DisplayName("OWNER는 그룹 채팅방 정보를 수정할 수 있다 (해피케이스)")
        void updateGroupChatInfo_success() {
            // setup
            Member owner = createMember(1L, "owner");
            Chat groupChat = createChat(100L, "oldName", ChatType.GROUP, null, 10);
            ChatMember chatMember = createChatMember(1L, groupChat, owner, ChatUserType.OWNER);
            GroupChatUpdateRequestDto updateDto = GroupChatUpdateRequestDto.of("newName", 15);

            // given
            given(chatMemberRepository.findChatMember(100L, owner.getId(), null, ChatType.GROUP, ChatUserType.OWNER))
                    .willReturn(Optional.of(chatMember));

            // when
            ChatInfoResponseDto result = chatRoomService.updateGroupChatInfo(100L, updateDto, owner.getId());

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("newName");
            assertThat(result.maxParticipant()).isEqualTo(15);

            // verify
            verify(chatMemberRepository, times(1))
                    .findChatMember(100L, owner.getId(), null, ChatType.GROUP, ChatUserType.OWNER);
        }

        @Test
        @DisplayName("OWNER가 아닌 사용자는 수정할 수 없다")
        void updateGroupChatInfo_unauthorized_throwsException() {
            // setup
            Member member = createMember(2L, "notOwner");

            // given
            given(chatMemberRepository.findChatMember(100L, member.getId(), null, ChatType.GROUP, ChatUserType.OWNER))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    chatRoomService.updateGroupChatInfo(100L,
                            GroupChatUpdateRequestDto.of("newName", 15), member.getId()))
                    .isInstanceOf(ChatException.class)
                    .hasMessageContaining("채팅방에 대한 권한이 없습니다.");

            // verify
            verify(chatMemberRepository, times(1))
                    .findChatMember(100L, member.getId(), null, ChatType.GROUP, ChatUserType.OWNER);
        }
    }


    @Nested
    @DisplayName("그룹 채팅방 나가기(leaveGroupChat) 테스트")
    class LeaveGroupChatTest {

        @Test
        @DisplayName("일반 멤버는 정상적으로 그룹 채팅방을 나갈 수 있다")
        void leaveGroupChat_success() {
            // setup
            Member member = createMember(1L, "member");
            Chat chat = createChat(200L, "groupChat", ChatType.GROUP, null, 10);
            ChatMember chatMember = createChatMember(1L, chat, member, ChatUserType.MEMBER);

            // given
            given(chatMemberRepository.findChatMember(200L, member.getId(), ChatStatus.OPENED, ChatType.GROUP, ChatUserType.MEMBER))
                    .willReturn(Optional.of(chatMember));

            // when
            chatRoomService.leaveGroupChat(200L, member.getId());

            // verify
            verify(chatMemberRepository, times(1))
                    .findChatMember(200L, member.getId(), ChatStatus.OPENED, ChatType.GROUP, ChatUserType.MEMBER);
            verify(chatMemberRepository, times(1)).delete(chatMember);
        }

        @Test
        @DisplayName("참여자가 아닌 경우 예외 발생")
        void leaveGroupChat_notParticipant_throwsException() {
            // setup
            Member member = createMember(1L, "member");

            // given
            given(chatMemberRepository.findChatMember(200L, member.getId(), ChatStatus.OPENED, ChatType.GROUP, ChatUserType.MEMBER))
                    .willReturn(Optional.empty());

            // when
            assertThatThrownBy(() -> chatRoomService.leaveGroupChat(200L, member.getId()))
                    .isInstanceOf(ChatException.class)
                    .hasMessageContaining("채팅방에 대한 권한이 없습니다.");

            // verify
            verify(chatMemberRepository, times(1))
                    .findChatMember(200L, member.getId(), ChatStatus.OPENED, ChatType.GROUP, ChatUserType.MEMBER);
            verify(chatMemberRepository, times(0)).delete(any());
        }
    }


    @Nested
    @DisplayName("그룹 채팅방 참여(joinGroupChat) 테스트")
    class JoinGroupChatTest {

        @Test
        @DisplayName("OPENED 상태의 그룹 채팅방에 정상적으로 참여한다")
        void joinGroupChat_success() {
            //setup
            Member member = createMember(1L, "member");
            Chat chat = createChat(300L, "chatName", ChatType.GROUP, null, 10);

            // given
            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
            given(chatRepository.findChatByIdAndType(300L, ChatType.GROUP)).willReturn(Optional.of(chat));
            given(chatMemberRepository.existsMemberInChat(300L, member.getId(), ChatStatus.OPENED)).willReturn(false);

            // when
            chatRoomService.joinGroupChat(300L, member.getId());

            // then
            assertThat(chat.getCurParticipant()).isEqualTo(3);
            // verify
            verify(chatRepository, times(1)).findChatByIdAndType(300L, ChatType.GROUP);
            verify(chatMemberRepository, times(1)).existsMemberInChat(300L, member.getId(), ChatStatus.OPENED);
            verify(chatMemberRepository, times(1)).save(any(ChatMember.class));
        }

        @Test
        @DisplayName("이미 참여 중이면 아무 작업도 수행하지 않는다")
        void joinGroupChat_alreadyJoined() {
            // setup
            Member member = createMember(1L, "member");
            Chat chat = createChat(300L, "chatName", ChatType.GROUP, null, 10);

            // given
            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
            given(chatRepository.findChatByIdAndType(300L, ChatType.GROUP)).willReturn(Optional.of(chat));
            given(chatMemberRepository.existsMemberInChat(300L, member.getId(), ChatStatus.OPENED)).willReturn(true);

            // when
            chatRoomService.joinGroupChat(300L, member.getId());

            // then
            assertThat(chat.getCurParticipant()).isEqualTo(2);

            // verify
            verify(chatMemberRepository, times(0)).save(any());
        }

        @Test
        @DisplayName("닫힌 채팅방이면 참여 시 예외 발생")
        void joinGroupChat_closedChat_throwsException() {
            // setup
            Member member = createMember(1L, "member");
            Chat chat = createChat(300L, "chatName", ChatType.GROUP, null, 10);
            ReflectionTestUtils.setField(chat, "status", ChatStatus.CLOSED);

            // given
            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
            given(chatRepository.findChatByIdAndType(300L, ChatType.GROUP)).willReturn(Optional.empty());

            // when
            assertThatThrownBy(() -> chatRoomService.joinGroupChat(300L, member.getId()))
                    .isInstanceOf(ChatException.class)
                    .hasMessageContaining("유효하지 않은 채팅방입니다.");

            // verify
            verify(chatRepository, times(1)).findChatByIdAndType(300L, ChatType.GROUP);
        }
    }


    @Nested
    @DisplayName("그룹 채팅방 닫기(closeGroupChat) 테스트")
    class CloseGroupChatTest {

        @Test
        @DisplayName("OWNER는 OPENED 상태의 채팅방을 닫을 수 있다(CLOSED)")
        void closeGroupChat_success() {
            // setup
            Member owner = createMember(1L, "owner");
            Chat chat = createChat(400L, "chatName", ChatType.GROUP, null, 10);
            ChatMember chatMember = createChatMember(1L, chat, owner, ChatUserType.OWNER);

            // given
            given(chatMemberRepository.findChatMember(400L, owner.getId(), ChatStatus.OPENED, ChatType.GROUP, ChatUserType.OWNER))
                    .willReturn(Optional.of(chatMember));

            // when
            chatRoomService.closeGroupChat(400L, owner.getId());

            // then
            assertThat(chat.getStatus()).isEqualTo(ChatStatus.CLOSED);

            // verify
            verify(chatMemberRepository, times(1))
                    .findChatMember(400L, owner.getId(), ChatStatus.OPENED, ChatType.GROUP, ChatUserType.OWNER);
        }

        @Test
        @DisplayName("OWNER가 아니면 닫을 수 없다")
        void closeGroupChat_unauthorized_throwsException() {
            // setup
            Member otherMember = createMember(1L, "otherMember");
            Chat chat = createChat(400L, "chatName", ChatType.GROUP, null, 10);

            // given
            given(chatMemberRepository.findChatMember(400L, otherMember.getId(), ChatStatus.OPENED, ChatType.GROUP, ChatUserType.OWNER))
                    .willReturn(Optional.empty());

            // when
            assertThatThrownBy(() ->
                    chatRoomService.closeGroupChat(400L, otherMember.getId())
            )
                    .isInstanceOf(ChatException.class)
                    .hasMessageContaining("채팅방에 대한 권한이 없습니다.");

            // verify
            verify(chatMemberRepository, times(1))
                    .findChatMember(400L, otherMember.getId(), ChatStatus.OPENED, ChatType.GROUP, ChatUserType.OWNER);
            verifyNoMoreInteractions(chatMemberRepository);
        }
    }


    @Nested
    @DisplayName("그룹 채팅방 열기(openGroupChat) 테스트")
    class OpenGroupChatTest {

        @Test
        @DisplayName("OWNER는 CLOSED 상태의 채팅방을 다시 열 수 있다")
        void openGroupChat_success() {
            // setup
            Member owner = createMember(1L, "owner");
            Chat chat = createChat(500L, "chatName", ChatType.GROUP, null, 10);
            ReflectionTestUtils.setField(chat, "status", ChatStatus.CLOSED);
            ChatMember chatMember = createChatMember(1L, chat, owner, ChatUserType.OWNER);

            // given
            given(chatMemberRepository.findChatMember(500L, owner.getId(), ChatStatus.CLOSED, ChatType.GROUP, ChatUserType.OWNER))
                    .willReturn(Optional.of(chatMember));

            // when
            chatRoomService.openGroupChat(500L, owner.getId());

            // then
            assertThat(chat.getStatus()).isEqualTo(ChatStatus.OPENED);

            // verify
            verify(chatMemberRepository, times(1))
                    .findChatMember(500L, owner.getId(), ChatStatus.CLOSED, ChatType.GROUP, ChatUserType.OWNER);
            verifyNoMoreInteractions(chatMemberRepository);
        }

        @Test
        @DisplayName("OWNER가 아니면 열 수 없다")
        void openGroupChat_unauthorized_throwsException() {
            // setup
            Member otherMember = createMember(2L, "notOwner");
            Chat chat = createChat(500L, "chatName", ChatType.GROUP, null, 10);
            ReflectionTestUtils.setField(chat, "status", ChatStatus.CLOSED);

            // given
            given(chatMemberRepository.findChatMember(500L, otherMember.getId(), ChatStatus.CLOSED, ChatType.GROUP, ChatUserType.OWNER))
                    .willReturn(Optional.empty());

            // when
            assertThatThrownBy(() -> chatRoomService.openGroupChat(500L, otherMember.getId()))
                    .isInstanceOf(ChatException.class)
                    .hasMessageContaining("채팅방에 대한 권한이 없습니다.");

            // then
            assertThat(chat.getStatus()).isEqualTo(ChatStatus.CLOSED);

            // verify
            verify(chatMemberRepository, times(1))
                    .findChatMember(500L, otherMember.getId(), ChatStatus.CLOSED, ChatType.GROUP, ChatUserType.OWNER);
            verifyNoMoreInteractions(chatMemberRepository);
        }
    }


    @Nested
    @DisplayName("그룹 채팅방 상세 조회(getGroupChatDetail) 테스트")
    class GetGroupChatDetailTest {

        @Test
        @DisplayName("그룹 채팅방 상세 정보를 정상적으로 조회할 수 있다")
        void getGroupChatDetail_success() {
            // setup
            Member owner = createMember(1L, "owner");
            Category category1 = createCategory(1L, ProductCategory.A_BOOTS);
            Category category2 = createCategory(2L, ProductCategory.M_LOAFERS);

            Chat chat = createGroupChatWithTags(600L, "패션 이야기방", owner, List.of(category1, category2));
            GroupChatDetailResponseDto result = GroupChatDetailResponseDto.of(chat);

            // given
            given(chatMemberRepository.existsChatMemberAndMemberIdBy(600L, owner.getId()))
                    .willReturn(true);
            given(chatRepository.findByIdAndType(600L, ChatType.GROUP))
                    .willReturn(Optional.of(chat));

            chatRoomService.getGroupChatDetail(600L, owner.getId());

            // then
            assertThat(result.chatId()).isEqualTo(600L);
            assertThat(result.name()).isEqualTo("패션 이야기방");
            assertThat(result.productCategoryList()).containsExactlyInAnyOrder("부츠", "로퍼");
            assertThat(result.memberList()).extracting("name").contains("owner");

            // verify
            verify(chatRepository, times(1)).findByIdAndType(600L, ChatType.GROUP);
        }

        @Test
        @DisplayName("채팅방 참여자가 아닌 경우")
        void getGroupChatDetail_unauthorizedParticipants_throwsException() {
            // setup
            Member otherMember = createMember(2L, "otherMember");
            Long chatId = 600L;

            // given
            given(chatMemberRepository.existsChatMemberAndMemberIdBy(chatId, otherMember.getId()))
                    .willReturn(false);

            // when
            assertThatThrownBy(() -> chatRoomService.getGroupChatDetail(chatId, otherMember.getId()))
                    .isInstanceOf(ChatException.class)
                    .hasMessageContaining("채팅방에 대한 권한이 없습니다.");

            // then
            verify(chatMemberRepository, times(1))
                    .existsChatMemberAndMemberIdBy(chatId, otherMember.getId());
            verifyNoInteractions(chatRepository);        }

        @Test
        @DisplayName("존재하지 않는 채팅방 조회 시 예외 발생")
        void getGroupChatDetail_notFound_throwsException() {
            // setup
            Long chatId = 600L;
            Long memberId = 1L;

            // given
            given(chatMemberRepository.existsChatMemberAndMemberIdBy(chatId, memberId))
                    .willReturn(true);
            given(chatRepository.findByIdAndType(chatId, ChatType.GROUP))
                    .willReturn(Optional.empty());

            // when
            assertThatThrownBy(() -> chatRoomService.getGroupChatDetail(chatId, memberId))
                    .isInstanceOf(ChatException.class)
                    .hasMessageContaining("채팅방을 찾을 수 없습니다.");

            // verify
            verify(chatMemberRepository, times(1))
                    .existsChatMemberAndMemberIdBy(chatId, memberId);
            verify(chatRepository, times(1))
                    .findByIdAndType(chatId, ChatType.GROUP);
        }
    }

}
