    package com.space.munova.product.domain;



    import com.space.munova.core.entity.BaseEntity;
    import com.space.munova.member.entity.Member;
    import com.space.munova.product.application.exception.ProductException;
    import jakarta.persistence.*;
    import lombok.*;
    import org.hibernate.annotations.ColumnDefault;

    import java.util.ArrayList;
    import java.util.List;

    @Builder
    @Entity
    @Table(name = "product")
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public class Product extends BaseEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "product_id")
        private Long id;
        private String info;
        private String name;
        private Long price;

        @ColumnDefault("0")
        @Builder.Default
        private Integer likeCount = 0;

        @ColumnDefault("0")
        @Builder.Default
        private Integer salesCount = 0;

        @ColumnDefault("0")
        @Builder.Default
        private Integer viewCount = 0;

        /// 단방향 매핑으로 설정.
        /// foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT) 외래키 제약 조건을 걸지않음
        @ManyToOne
        @JoinColumn(name = "brand_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
        private Brand brand;

        @ManyToOne
        @JoinColumn(name = "product_category_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
        private Category category;

        @ManyToOne
        @JoinColumn(name = "member_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
        private Member member;

        @ColumnDefault("0")
        private boolean isDeleted;

        @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
        @Builder.Default
        private List<ProductImage> productImages = new ArrayList<>();

        @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
        @Builder.Default
        private List<ProductDetail> productDetails = new ArrayList<>();

        public static Product createDefaultProduct(String name,
                                                   String info,
                                                   Long price,
                                                   Brand brand,
                                                   Category category,
                                                   Member member
                                                   ) {

            if(brand == null) {
                throw new IllegalArgumentException("브랜드는 null일 수 없습니다.");
            }
            if(category == null) {
                throw new IllegalArgumentException("카테고리는 null일 수 없습니다.");
            }
            if(price == null){
                throw new IllegalArgumentException("가격은 null일 수 없습니다.");
            }
            if(price < 0) {
                throw new IllegalArgumentException("가격은 음수일 수 없습니다.");
            }
            if(info == null ||  info.isEmpty() || info.length() < 10) {
                throw new IllegalArgumentException("상품 정보는 최소 10자 이상이어야 합니다.");
            }
            if(info.length() > 65535) {
                throw new IllegalArgumentException("상품 정보는 최대 65535자까지 입력 가능합니다.");
            }
            if(name == null || name.isEmpty()) {
                throw new IllegalArgumentException("상품명은 null이거나 비어있을 수 없습니다.");
            }
            if(member == null) {
                throw new IllegalArgumentException("회원은 null일 수 없습니다.");
            }

            return Product.builder()
                    .brand(brand)
                    .info(info)
                    .price(price)
                    .category(category)
                    .name(name)
                    .member(member)
                    .build();
        }


        public void updateProduct(String name,
                                  String info,
                                  Long price) {

            if(price < 0) {
                throw ProductException.badRequestException("가격은 0보다 작을수 없습니다.");
            }
            if(name == null || name.isEmpty()) {
                throw ProductException.badRequestException("상품명은 null이거나 비어있을 수 없습니다.");
            }
            if(info == null || info.isEmpty()) {
                throw ProductException.badRequestException("상품정보는 null이거나 비어있을 수 없습니다.");
            }
            if(info.length() < 10) {
                throw ProductException.badRequestException("상품정보는 최소 10자 이상이어야 합니다.");
            }
            if(info.length() > 65535) {
                throw ProductException.badRequestException("상품정보는 최대 65535자까지 입력 가능합니다.");
            }
            this.name = name;
            this.info = info;
            this.price = price;
        }

        /// 상품 논리적 제거
        public void deleteProduct() {
            this.isDeleted = true;
        }

        /// 좋아요 감소
        public void minusLike() {
            if(this.likeCount <= 0) {
                throw ProductException.badRequestException("좋아요 수는 음수일 수 없습니다.");
            }
            this.likeCount -= 1;
        }

        /// 좋아요 증가
        public void plusLike() {
            this.likeCount += 1;
        }

        /// 판매량 감소
        public void minusSalesCount(int salesCount) {
            if(salesCount < 0) {
                throw ProductException.badRequestException("차감할 판매량은 음수일 수 없습니다.");
            }
            if(this.salesCount - salesCount < 0) {
                throw ProductException.badRequestException("판매량은 음수일 수 없습니다.");
            }

            this.salesCount -= salesCount;
        }

        /// 판매량 증가.
        public void plusSalesCount(int salesCount) {
            this.salesCount += salesCount;
        }


    }
