package com.space.munova.product.domain.product;



import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Builder
@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Product  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String info;
    private String name;
    private Long price;

    @ColumnDefault("0")
    private Integer likeCount;

    @ColumnDefault("0")
    private Integer salesCount;


    /// 단방향 매핑으로 설정.
    /// foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT) 외래키 제약 조건을 걸지않음
    @ManyToOne
    @JoinColumn(name = "brand_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Brand brand;

    @ManyToOne
    @JoinColumn(name = "product_category_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Category category;

//    @ManyToOne
//    @JoinColumn(name = "product_category_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
//    private User user;

    @ColumnDefault("0")
    private boolean isDeleted;

    public static Product createDefaultProduct(String name,
                                               String info,
                                               Long price,
                                               Brand brand,
                                               Category category
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

        return Product.builder()
                .brand(brand)
                .info(info)
                .price(price)
                .category(category)
                .name(name)
                .build();
    }


    /// 상품 논리적 제거
    public void deleteProduct() {
        this.isDeleted = true;
    }

    /// 좋아요 감소
    public void minusLike() {
        if(this.likeCount < 0) {
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
