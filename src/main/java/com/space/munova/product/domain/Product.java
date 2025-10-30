    package com.space.munova.product.domain;



    import com.space.munova.core.entity.BaseEntity;
    import com.space.munova.member.entity.Member;
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
                throw new IllegalArgumentException("brand cannot be null");
            }
            if(category == null) {
                throw new IllegalArgumentException("category cannot be null");
            }
            if(price == null){
                throw new IllegalArgumentException("price cannot be null");
            }
            if(price < 0) {
                throw new IllegalArgumentException("price cannot be negative");
            }
            if(info == null ||  info.isEmpty() || info.length() < 10) {
                throw new IllegalArgumentException("product information must be at least 10 characters long.");
            }
            if(info.length() > 65535) {
                throw new IllegalArgumentException("Product information is too long. A maximum of 65535 characters is allowed.");
            }
            if(name == null || name.isEmpty()) {
                throw new IllegalArgumentException("name cannot be null or empty");
            }
            if(member == null) {
                throw new IllegalArgumentException("member cannot be null");
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
                throw new IllegalArgumentException("가격은 0보다 작을수 없습니다.");
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
                throw new IllegalArgumentException("like count cannot be negative");
            }
            this.likeCount -= 1;
        }

        /// 좋아요 증가
        public void plusLike() {
            this.likeCount += 1;
        }

        /// 판매량 감소
        public void minusSalesCount(int salesCount) {
            if(this.salesCount - salesCount < 0) {
                throw new IllegalArgumentException("salesCount cannot be negative");
            }

            this.salesCount -= salesCount;
        }

        /// 판매량 증가.
        public void plusSalesCount(int salesCount) {
            this.salesCount += salesCount;
        }

    }
