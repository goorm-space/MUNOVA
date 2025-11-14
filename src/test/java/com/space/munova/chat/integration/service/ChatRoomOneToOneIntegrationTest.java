package com.space.munova.chat.integration.service;

import com.space.munova.IntegrationTestBase;
import com.space.munova.chat.dto.ChatItemDto;
import com.space.munova.chat.dto.group.ChatInfoResponseDto;
import com.space.munova.chat.dto.onetoone.OneToOneChatResponseDto;
import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.ChatMember;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import com.space.munova.chat.enums.ChatUserType;
import com.space.munova.chat.exception.ChatException;
import com.space.munova.chat.repository.ChatMemberRepository;
import com.space.munova.chat.repository.ChatRepository;
import com.space.munova.chat.service.ChatRoomService;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Transactional
public class ChatRoomOneToOneIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ChatRoomService chatRoomService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private ChatMemberRepository chatMemberRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;

    private Member createMember(String username) {
        return memberRepository.save(Member.createMember(username, "0000", "SEOUL"));
    }

    private Category createCategory(ProductCategory type) {
        return categoryRepository.save(
                new Category(null, null, type, type.getDescription(), 1)
        );
    }

    private Product createProduct(String name, Member seller) {
        Category category = createCategory(ProductCategory.A_WINTER_BOOTS);
        Brand brand = new Brand(null, "nike", "logo", "nike desc"); // 매핑에 따라 조정 필요할 수 있음
        return productRepository.save(
                Product.createDefaultProduct(name, "product information must be at least 10 characters long",
                        10000L, brand, category, seller)
        );
    }

    @Test
    @DisplayName("1:1 채팅방 생성 통합 테스트 - 해피케이스")
    void createOneToOneChatRoom_success() {
        // given
        Member seller = createMember("seller");
        Member buyer = createMember("buyer");
        Product product = createProduct("신발", seller);

        // when
        OneToOneChatResponseDto result = chatRoomService.createOneToOneChatRoom(product.getId(), buyer.getId());

        // then
        assertThat(result).isNotNull();
        assertThat(result.buyerId()).isEqualTo(buyer.getId());
        assertThat(result.sellerId()).isEqualTo(seller.getId());

        Chat chat = chatRepository.findById(result.chatId()).orElseThrow();
        assertThat(chat.getType()).isEqualTo(ChatType.ONE_ON_ONE);
        assertThat(chat.getStatus()).isEqualTo(ChatStatus.OPENED);
        assertThat(chat.getChatMembers()).hasSize(2);

        List<ChatMember> members = chat.getChatMembers();
        assertThat(members)
                .extracting(ChatMember::getChatMemberType)
                .containsExactlyInAnyOrder(ChatUserType.OWNER, ChatUserType.MEMBER);
    }
//
//    @Test
//    @DisplayName("이미 존재하는 1:1 채팅방이 있으면 새로 만들지 않고 기존 채팅방 정보를 반환한다")
//    void createOneToOneChatRoom_existingChat_returnsExisting() {
//        // given
//        Member seller = createMember("seller");
//        Member buyer = createMember("buyer");
//        Product product = createProduct("신발", seller);
//
//        // 첫 번째 호출로 채팅방 생성
//        OneToOneChatResponseDto first = chatRoomService.createOneToOneChatRoom(product.getId(), buyer.getId());
//
//        // when – 두 번째 호출
//        OneToOneChatResponseDto second = chatRoomService.createOneToOneChatRoom(product.getId(), buyer.getId());
//
//        // then
//        assertThat(second.chatId()).isEqualTo(first.chatId());
//        assertThat(chatRepository.count()).isEqualTo(1L);
//        assertThat(chatMemberRepository.count()).isEqualTo(2L);
//    }
//
//    @Test
//    @DisplayName("구매자와 판매자가 동일하면 1:1 채팅방 생성 시 예외가 발생한다")
//    void createOneToOneChatRoom_selfChat_throwsException() {
//        // given
//        Member self = createMember("sameUser");
//        Product product = createProduct("신발", self);
//
//        // when & then
//        assertThatThrownBy(() -> chatRoomService.createOneToOneChatRoom(product.getId(), self.getId()))
//                .isInstanceOf(ChatException.class)
//                .hasMessageContaining("자신이 등록한 상품에는 문의할 수 없습니다.");
//    }
//
//    @Test
//    @DisplayName("1:1 채팅방 목록 조회 - 구매자 기준(OPENED만)")
//    void getOneToOneChatRoomsByMember_buyer() {
//        // given
//        Member seller = createMember("seller");
//        Member buyer = createMember("buyer");
//        Product product = createProduct("신발", seller);
//
//        OneToOneChatResponseDto chatDto =
//                chatRoomService.createOneToOneChatRoom(product.getId(), buyer.getId());
//
//        // when
//        List<ChatItemDto> list =
//                chatRoomService.getOneToOneChatRoomsByMember(ChatUserType.MEMBER, buyer.getId());
//
//        // then
//        assertThat(list).hasSize(1);
//        ChatItemDto item = list.getFirst();
//        assertThat(item.chatId()).isEqualTo(chatDto.chatId());
//        assertThat(item.name()).contains("문의");
//    }
//
//    @Test
//    @DisplayName("1:1 채팅방 목록 조회 - 판매자 기준(OWNER)")
//    void getOneToOneChatRoomsBySeller() {
//        // given
//        Member seller = createMember("seller");
//        Member buyer = createMember("buyer");
//        Product product = createProduct("신발", seller);
//
//        chatRoomService.createOneToOneChatRoom(product.getId(), buyer.getId());
//
//        // when
//        List<ChatItemDto> list = chatRoomService.getOneToOneChatRoomsBySeller(seller.getId());
//
//        // then
//        assertThat(list).hasSize(1);
//        assertThat(list.getFirst().name()).contains("문의");
//    }
//
//    @Test
//    @DisplayName("판매자는 OPENED 상태의 1:1 채팅방을 닫을 수 있다")
//    void setChatRoomClosed_success() {
//        // given
//        Member seller = createMember("seller");
//        Member buyer = createMember("buyer");
//        Product product = createProduct("신발", seller);
//
//        OneToOneChatResponseDto chatDto =
//                chatRoomService.createOneToOneChatRoom(product.getId(), buyer.getId());
//
//        // when
//        ChatInfoResponseDto result =
//                chatRoomService.setChatRoomClosed(chatDto.chatId(), seller.getId(), MemberRole.SELLER);
//
//        // then
//        assertThat(result.status()).isEqualTo(ChatStatus.CLOSED);
//
//        Chat chat = chatRepository.findById(chatDto.chatId()).orElseThrow();
//        assertThat(chat.getStatus()).isEqualTo(ChatStatus.CLOSED);
//    }
//
//    @Test
//    @DisplayName("구매자는 1:1 채팅방을 닫을 수 없다")
//    void setChatRoomClosed_unauthorized() {
//        // given
//        Member seller = createMember("seller");
//        Member buyer = createMember("buyer");
//        Product product = createProduct("신발", seller);
//
//        OneToOneChatResponseDto chatDto =
//                chatRoomService.createOneToOneChatRoom(product.getId(), buyer.getId());
//
//        // when & then
//        assertThatThrownBy(() ->
//                chatRoomService.setChatRoomClosed(chatDto.chatId(), buyer.getId(), MemberRole.USER)
//        )
//                .isInstanceOf(ChatException.class)
//                .hasMessageContaining("채팅방에 대한 권한이 없습니다.");
//    }
}