package com.space.munova.order.repository;

import com.space.munova.IntegrationTestBase;
import com.space.munova.member.entity.Member;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.product.domain.Repository.ProductDetailRepository;
import com.space.munova.product.domain.Repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;


@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public class OrderRepositoryTest extends IntegrationTestBase {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductDetailRepository productDetailRepository;

    @Autowired
    private MemberRepository memberRepository;

    @DisplayName("order 저장")
    @Test
    void saveOrder_persistsOrderAndOrderItems() {
        // given
        Member member = memberRepository.save(Member.createMember("user","password", "address"));
        Product product = productRepository.save(Product.builder().name("테스트상품").price(10000L).build());
        ProductDetail pd = productDetailRepository.save(ProductDetail.builder().quantity(100).product(product).build());

        // build order + orderItem
        Order order = Order.createInitOrder(member, "문 앞에 놔주세요");
        OrderItem item = OrderItem.builder()
                .order(order) // set owning side explicitly
                .productDetail(pd)
                .nameSnapshot(product.getName())
                .priceSnapshot(product.getPrice())
                .quantity(2)
                .status(OrderStatus.CREATED)
                .build();

        // attach to order (depending on your addOrderItem impl you might want to call addOrderItem)
        order.getOrderItems().add(item);

        // when
        Order saved = orderRepository.saveAndFlush(order);

        // then - id assigned, cascade saved child, FK set
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderItems()).hasSize(1);

        OrderItem savedItem = saved.getOrderItems().get(0);
        assertThat(savedItem.getId()).isNotNull();
        assertThat(savedItem.getProductDetail().getId()).isEqualTo(pd.getId());
        assertThat(savedItem.getOrder().getId()).isEqualTo(saved.getId());

        // when

        // then

    }
}
