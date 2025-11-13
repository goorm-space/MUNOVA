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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@Testcontainers
public class GroupChatMemberRepository extends IntegrationTestBase {

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
    private Chat groupChat1;
    private Chat groupChat2;
    private ChatMember chatMember1;
    private ChatMember chatMember2;


    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.createMember("testUser", "0000", "SEOUL"));
        // 채팅 생성
        groupChat1 = chatRepository.save(
                Chat.createChat("groupChat1", ChatStatus.OPENED, ChatType.GROUP, null, 5, 10));
        groupChat2 = chatRepository.save(
                Chat.createChat("groupChat2", ChatStatus.OPENED, ChatType.GROUP, null, 5, 10));

        chatMember1 = chatMemberRepository.save(ChatMember.createChatMember(groupChat1, member, ChatUserType.MEMBER, member.getUsername()));
        chatMember2 = chatMemberRepository.save(ChatMember.createChatMember(groupChat2, member, ChatUserType.MEMBER, member.getUsername()));

    }


    @Nested
    @DisplayName("findGroupChats 테스트")
    class FindGroupChatsEntityTest {

//        @Test
//        @DisplayName("GROUP 타입의 OPENED 채팅만 조회되어야 함")
//        void shouldFindOnlyGroupChats() {
//            // when
//            List<Chat> result = chatRepository.findGroupChats(
//                    member.getId(),
//                    ChatType.GROUP,
//                    ChatStatus.OPENED
//            );
//
//            // then
//            assertThat(result)
//                    .hasSize(2)
//                    .allMatch(chat -> chat.getType() == ChatType.GROUP)
//                    .allMatch(chat -> chat.getStatus() == ChatStatus.OPENED);
//
//            assertThat(result)
//                    .extracting(Chat::getName)
//                    .containsExactlyInAnyOrder("groupChat1", "groupChat2");
//
//            assertThat(result)
//                    .extracting(Chat::getId)
//                    .containsExactlyInAnyOrder(groupChat1.getId(), groupChat2.getId());
//
//            assertThat(result)
//                    .allSatisfy(chat -> {
//                        assertThat(chat.getCreatedAt()).isNotNull();
//                        assertThat(chat.getId()).isPositive();
//                        assertThat(chat.getName()).startsWith("groupChat");
//                    });
//
//            assertThat(result).hasSize(2);
//            assertThat(result.get(0).getCreatedAt())
//                    .isAfterOrEqualTo(result.get(1).getCreatedAt());
//        }

        @Test
        @DisplayName("정렬 기준: DESC 순서 - LastMessage가 있는 경우")
        void shouldReturnChatsOrderedByLastMessageTimeDesc() throws InterruptedException {

            groupChat1.modifyLastMessageContent("lastMessageContent", LocalDateTime.now());
            chatRepository.save(groupChat1);
            // when
            List<Chat> result = chatRepository.findByChatTypeAndChatStatus(
                    member.getId(),
                    ChatType.GROUP,
                    null,
                    ChatStatus.OPENED
            );

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getLastMessageTime()).isNotNull(); // chat1이 먼저 와야 함
            assertThat(result.get(0).getId()).isEqualTo(groupChat1.getId());
            assertThat(result.get(1).getId()).isEqualTo(groupChat2.getId());
            assertThat(result.get(0).getLastMessageTime())
                    .isAfterOrEqualTo(result.get(1).getCreatedAt());
        }



        @Test
        @DisplayName("멤버가 속하지 않은 그룹 채팅은 조회되지 않아야 함")
        void shouldNotReturnChatsUserIsNotMemberOf() {
            // given
            Chat groupChat2 = chatRepository.save(Chat.createChat("groupChat3", ChatStatus.OPENED, ChatType.GROUP, null, 5, 10));

            // when
            List<Chat> result = chatRepository.findByChatTypeAndChatStatus(
                    member.getId(),
                    ChatType.GROUP,
                    null,
                    ChatStatus.OPENED
            );

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("CLOSED 상태의 채팅은 조회되지 않아야 함")
        void shouldNotReturnClosedChats() {
            // given
            groupChat1.updateChatStatus(ChatStatus.CLOSED);
            chatRepository.save(groupChat1);

            // when
            List<Chat> result = chatRepository.findByChatTypeAndChatStatus(
                    member.getId(),
                    ChatType.GROUP,
                    null,
                    ChatStatus.OPENED
            );

            // then
            assertThat(result).hasSize(1);
        }
    }


}
