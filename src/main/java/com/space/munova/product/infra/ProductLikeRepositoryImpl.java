package com.space.munova.product.infra;


import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.domain.Repository.ProductLikeRepositoryCustom;
import com.space.munova.product.domain.enums.ProductImageType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;
import static com.space.munova.member.entity.QMember.member;
import static com.space.munova.product.domain.QBrand.brand;
import static com.space.munova.product.domain.QProduct.product;
import static com.space.munova.product.domain.QProductImage.productImage;
import static com.space.munova.product.domain.QProductLike.productLike;

@Repository
@RequiredArgsConstructor
public class ProductLikeRepositoryImpl implements ProductLikeRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    @Override
    public List<FindProductResponseDto> findLikeProductByMemberId(Pageable pageable, Long memberId) {

        return queryFactory
                .select(Projections.constructor(FindProductResponseDto.class,
                        product.id.as("productId"),
                        productImage.savedName.as("mainImgSrc"),
                        brand.brandName.as("brandName"),
                        product.name.as("productName"),
                        product.price.as("price"),
                        product.likeCount.as("likeCount"),
                        product.salesCount.as("salesCount"),
                        productLike.createdAt.as("createAt")
                        ))
                .from(productLike)
                .join(product)
                .on(productLike.product.id.eq(product.id))
                .join(member)
                .on(productLike.member.id.eq(member.id))
                .join(productImage)
                .on(productImage.product.id.eq(product.id)
                        .and(productImage.imageType.eq(ProductImageType.MAIN)))
                .join(brand)
                .on(brand.id.eq(product.brand.id))
                .where(productLike.member.id.eq(memberId)
                        .and(productLike.isDeleted.eq(false))
                        .and(product.isDeleted.eq(false)))
                .orderBy(productLike.createdAt.desc())
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetch();
    }
}
