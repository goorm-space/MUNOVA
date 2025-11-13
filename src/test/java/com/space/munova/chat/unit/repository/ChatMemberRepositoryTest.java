package com.space.munova.chat.unit.repository;

import com.space.munova.IntegrationTestBase;
import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.ChatMember;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import com.space.munova.chat.enums.ChatUserType;
import com.space.munova.chat.repository.ChatMemberRepository;
import com.space.munova.chat.repository.ChatRepository;
import com.space.munova.member.entity.Member;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.domain.Brand;
import com.space.munova.product.domain.Category;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.Repository.BrandRepository;
import com.space.munova.product.domain.Repository.CategoryRepository;
import com.space.munova.product.domain.Repository.ProductRepository;
import com.space.munova.product.domain.enums.ProductCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
@Testcontainers
public class ChatMemberRepositoryTest extends IntegrationTestBase {

    @Autowired
    private ChatMemberRepository chatMemberRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Member member;
    private Member seller;
    private Product product;
    private Category category;
    private Brand brand;
    private Chat chat;
    private ChatMember chatMember;
    private ChatMember chatSeller;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.createMember("testUser", "0000", "SEOUL"));
        seller = memberRepository.save(Member.createMember("testSeller", "0000", "SEOUL"));
        brand = brandRepository.save(new Brand(null,"nike", "1", "nike"));
        category = categoryRepository.save(new Category(null, null, ProductCategory.A_BOOTS, "부츠", 1));
        product = productRepository.save(Product.createDefaultProduct("product", "product information must be at least 10 characters long", 10000L, brand, category, seller));

        // 채팅 생성
        chat = chatRepository.save(
                Chat.createChat("testChat", ChatStatus.OPENED, ChatType.ONE_ON_ONE, product, 2, 2));

        chatMember = chatMemberRepository.save(ChatMember.createChatMember(chat, member, ChatUserType.MEMBER, member.getUsername()));
        chatSeller = chatMemberRepository.save(ChatMember.createChatMember(chat, seller, ChatUserType.OWNER, seller.getUsername()));
    }


    // save 테스트
    @Nested
    @DisplayName("Save 테스트")
    class SaveChatMember {
        @Test
        @DisplayName("ChatMember 저장")
        void saveChatMember() {
            // given
            Member newMember = memberRepository.save(Member.createMember("newMember", "0000", "SEOUL"));
            Chat newChat = chatRepository.save(Chat.createChat("newChat", ChatStatus.OPENED, ChatType.ONE_ON_ONE, product, 2, 2));
            ChatMember newChatMember =ChatMember.createChatMember(newChat, newMember, ChatUserType.MEMBER, newMember.getUsername());

            // when
            ChatMember saveChatMember = chatMemberRepository.save(newChatMember);

            // then
            assertThat(saveChatMember).isNotNull();
            assertThat(saveChatMember.getId()).isNotNull();
            assertThat(saveChatMember.getMemberId().getId()).isEqualTo(newMember.getId());
            assertThat(newChat.getProductId().getId()).isEqualTo(product.getId());
            assertThat(saveChatMember.getChatMemberType()).isEqualTo(ChatUserType.MEMBER);
        }
    }


    @Nested
    @DisplayName("1:1 채팅방 조회 테스트")   // JPQL 쿼리가 정확한 정보를 전달하는지 테스트
    class FindTests {

        @Test
        @Transactional
        @DisplayName("특정 상품과 멤버에 대한 채팅 존재 여부 - 해피케이스")
        void existingChatRoomFound(){
            Optional<ChatMember> existingChatRoom = chatMemberRepository.findExistingChatRoom(member.getId(), product.getId());

            assertTrue(existingChatRoom.isPresent());   // 채팅방 존재 여부
            assertEquals(chat.getId(), existingChatRoom.get().getChatId().getId());     // 채팅방 일치 여부
            assertEquals(chatMember.getMemberId().getId(), existingChatRoom.get().getMemberId().getId());  // 채팅방 멤버 여부 확인
            assertEquals(ChatStatus.OPENED, existingChatRoom.get().getChatId().getStatus());    // 채팅방 상태 OPENED 확인
        }

        @Test
        @DisplayName("채팅방 멤버가 아닌 경우")
        void noChatRoomFound(){
            Member otherMember = memberRepository.save(Member.createMember("otherMember", "0000", "SEOUL"));
            Optional<ChatMember> existingChatRoom = chatMemberRepository.findExistingChatRoom(otherMember.getId(), product.getId());
            assertTrue(existingChatRoom.isEmpty());   // 채팅방 존재 여부 -> 없어야지 당연히
        }

        @Test
        @DisplayName("상품이 없는 경우")
        void noProductIdFound(){
            Product otherProduct = productRepository.save(Product.createDefaultProduct("otherproduct", "product information must be at least 10 characters long", 10000L, brand, category, seller));
            Optional<ChatMember> existingChatRoom = chatMemberRepository.findExistingChatRoom(member.getId(), otherProduct.getId());
            assertTrue(existingChatRoom.isEmpty());   // 채팅방 존재 여부 -> 없어야지 당연히
        }

        @Test
        @DisplayName("CLOSED인 경우")
        void chatStatusClosedFound(){

            chat.updateChatStatus(ChatStatus.CLOSED);
            chatRepository.save(chat);
            Optional<ChatMember> existingChatRoom = chatMemberRepository.findExistingChatRoom(chat.getId(), product.getId());
            assertTrue(existingChatRoom.isEmpty());   // 채팅방 존재 여부 -> 없어야지 당연히
        }
    }

    // findAllChats 테스트
    @Nested
    @DisplayName("findAllChats 테스트 (구매자/판매자 1:1 채팅 조회)")
    class FindAllChatsTest {

        @Test
        @DisplayName("Member가 MEMBER 타입으로 조회하면 1개 반환")
        void buyerFindsHisChats() {

            var result = chatRepository.findByChatTypeAndChatStatus(
                    member.getId(),
                    ChatType.ONE_ON_ONE,
                    ChatUserType.MEMBER,
                    ChatStatus.OPENED
            );

            assertThat(result).hasSize(1);
            Chat chatResult = result.getFirst();

            assertThat(chatResult.getId()).isEqualTo(chat.getId());
            assertThat(chatResult.getStatus()).isEqualTo(ChatStatus.OPENED);
            assertThat(chatResult.getType()).isEqualTo(ChatType.ONE_ON_ONE);
            assertThat(chatResult.getProductId().getId()).isEqualTo(product.getId());
        }

        @Test
        @DisplayName("Member가 OWNER 타입으로 조회하면 0개")
        void buyerShouldNotSeeOwnerChats() {
            var result = chatRepository.findByChatTypeAndChatStatus(
                    member.getId(),
                    ChatType.ONE_ON_ONE,
                    ChatUserType.OWNER,
                    ChatStatus.OPENED
            );

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Member가 GROUP 채팅으로 조회하면 0개")
        void cannotGetGroupChats() {
            var result = chatRepository.findByChatTypeAndChatStatus(
                    member.getId(),
                    ChatType.GROUP,
                    ChatUserType.MEMBER,
                    ChatStatus.OPENED
            );

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Seller가 OWNER 타입으로 조회하면 1개 반환")
        void sellerFindsHisChats() {
            var result = chatRepository.findByChatTypeAndChatStatus(
                    seller.getId(),
                    ChatType.ONE_ON_ONE,
                    ChatUserType.OWNER,
                    ChatStatus.OPENED
            );

            assertThat(result).hasSize(1);
            Chat chatResult = result.getFirst();

            assertThat(chatResult.getId()).isEqualTo(chat.getId());
            assertThat(chatResult.getType()).isEqualTo(ChatType.ONE_ON_ONE);
            assertThat(chatResult.getStatus()).isEqualTo(ChatStatus.OPENED);
        }

        @Test
        @DisplayName("Seller가 MEMBER 타입으로 조회하면 0개")
        void sellerShouldNotSeeMemberChats() {
            var result = chatRepository.findByChatTypeAndChatStatus(
                    seller.getId(),
                    ChatType.ONE_ON_ONE,
                    ChatUserType.MEMBER,
                    ChatStatus.OPENED
            );

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ChatStatus = CLOSED 인 경우 조회되지 않아야 함")
        void closedChatsShouldNotBeReturned() {
            // given
            chat.updateChatStatus(ChatStatus.CLOSED);
            chatRepository.save(chat);

            // when
            var result = chatRepository.findByChatTypeAndChatStatus(
                    chat.getId(),
                    ChatType.ONE_ON_ONE,
                    ChatUserType.MEMBER,
                    ChatStatus.OPENED
            );

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("다른 ChatType (GROUP) 은 조회되지 않아야 함")
        void groupChatsShouldNotBeReturnedInOneToOneQuery() {
            // given
            Chat groupChat = chatRepository.save(
                    Chat.createChat("groupChat", ChatStatus.OPENED, ChatType.GROUP, null, 5, 10)
            );
            chatMemberRepository.save(ChatMember.createChatMember(groupChat, member, ChatUserType.MEMBER, member.getUsername()));

            // when
            var result = chatRepository.findByChatTypeAndChatStatus(
                    member.getId(),
                    ChatType.ONE_ON_ONE, // OneToOne만 조회
                    ChatUserType.MEMBER,
                    ChatStatus.OPENED
            );

            // then
            assertThat(result).hasSize(1); // 기존 one-to-one만 나와야 함
            assertThat(result.getFirst().getType()).isEqualTo(ChatType.ONE_ON_ONE);
        }
    }


    // delete 테스트
    @Nested
    @DisplayName("Delete 테스트")
    class DeleteChatMember {
        @Test
        @DisplayName("ChatMember 삭제")
        void deleteChatMember() {
            Member newMember = Member.createMember("newMember", "0000", "SEOUL");
            Chat newChat = Chat.createChat("newChat", ChatStatus.OPENED, ChatType.ONE_ON_ONE,  product,2, 2);
            ChatMember newChatMember = ChatMember.createChatMember(newChat, newMember, ChatUserType.MEMBER, newMember.getUsername());

            chatMemberRepository.delete(chatMember);

            Optional<ChatMember> deletedChatMember = chatMemberRepository.findById(chatMember.getId());
            assertThat(deletedChatMember).isEmpty();
        }
    }

}
