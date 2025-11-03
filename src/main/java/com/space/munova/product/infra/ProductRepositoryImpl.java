package com.space.munova.product.infra;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.domain.Repository.ProductRepositoryCustom;
import com.space.munova.product.domain.enums.ProductImageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import java.util.List;
import static com.space.munova.product.domain.QBrand.brand;
import static com.space.munova.product.domain.QCategory.category;
import static com.space.munova.product.domain.QOption.option;
import static com.space.munova.product.domain.QProduct.product;
import static com.space.munova.product.domain.QProductDetail.productDetail;
import static com.space.munova.product.domain.QProductImage.productImage;
import static com.space.munova.product.domain.QProductOptionMapping.productOptionMapping;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<FindProductResponseDto> findProductByConditions(Long categoryId, List<Long> optionIds, String keyword, Pageable pageable) {

        Long total = queryFactory
                .select(product.id.countDistinct())
                .from(product)
                .leftJoin(category)
                .on(category.id.eq(product.category.id))
                .leftJoin(productDetail)
                .on(product.id.eq(productDetail.product.id))
                .leftJoin(productOptionMapping)
                .on(productDetail.id.eq(productOptionMapping.productDetail.id))
                .leftJoin(option)
                .on(option.id.eq(productOptionMapping.option.id))
                .where(andCategory(categoryId),
                        andOption(optionIds),
                        searchKeywords(keyword),
                        product.isDeleted.eq(false)
                )
                .fetchOne();


        List<FindProductResponseDto> findProductByConditions = queryFactory
                .select(Projections.constructor(FindProductResponseDto.class,
                        product.id.as("productId"),
                        productImage.imgUrl.as("mainImgSrc"),
                        brand.brandName.as("brandName"),
                        product.name.as("productName"),
                        product.price.as("price"),
                        product.likeCount.as("likeCount"),
                        product.salesCount.as("salesCount"),
                        product.createdAt.as("createAt")
                        ))
                .from(product)
                .leftJoin(productImage)
                .on(product.id.eq(productImage.product.id)
                        .and(productImage.imageType.eq(ProductImageType.MAIN)))
                .leftJoin(category)
                .on(category.id.eq(product.category.id))
                .leftJoin(brand)
                .on(brand.id.eq(product.brand.id))
                .leftJoin(productDetail)
                .on(product.id.eq(productDetail.product.id))
                .leftJoin(productOptionMapping)
                .on(productDetail.id.eq(productOptionMapping.productDetail.id))
                .leftJoin(option)
                .on(option.id.eq(productOptionMapping.option.id))
                .where(andCategory(categoryId),
                        andOption(optionIds),
                        searchKeywords(keyword),
                        product.isDeleted.eq(false)
                        )
                .distinct()
                .orderBy(product.createdAt.desc())
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetch();

        return new PageImpl<>(findProductByConditions, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<FindProductResponseDto> findProductBySeller(Pageable pageable, Long sellerId) {

        List<FindProductResponseDto> content = queryFactory
                .select(Projections.constructor(FindProductResponseDto.class,
                        product.id.as("productId"),
                        productImage.imgUrl.as("mainImgSrc"),
                        brand.brandName.as("brandName"),
                        product.name.as("productName"),
                        product.price.as("price"),
                        product.likeCount.as("likeCount"),
                        product.salesCount.as("salesCount"),
                        product.createdAt.as("createAt")
                ))
                .from(product)
                .leftJoin(productImage)
                .on(product.id.eq(productImage.product.id)
                        .and(productImage.imageType.eq(ProductImageType.MAIN)))
                .leftJoin(category)
                .on(category.id.eq(product.category.id))
                .leftJoin(brand)
                .on(brand.id.eq(product.brand.id))
                .leftJoin(productDetail)
                .on(product.id.eq(productDetail.product.id))
                .leftJoin(productOptionMapping)
                .on(productDetail.id.eq(productOptionMapping.productDetail.id))
                .leftJoin(option)
                .on(option.id.eq(productOptionMapping.option.id))
                .where(product.member.id.eq(sellerId),
                        product.isDeleted.eq(false)
                )
                .distinct()
                .orderBy(product.createdAt.desc())
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetch();


        Long total = queryFactory
                .select(product.count())
                .from(product)
                .where(product.member.id.eq(sellerId)
                        .and(product.isDeleted.eq(false)))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }


    private Predicate searchKeywords(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }

        BooleanBuilder builder = new BooleanBuilder();
        builder.or(likeProductName(keyword));   // 상품명
        builder.or(likeCategoryName(keyword));  // 카테고리 타입(Enum)
        builder.or(likeOptionName(keyword));    // 옵션명 (서브쿼리)

        return builder;
    }



    private BooleanExpression andCategory(Long categoryId) {

        if(categoryId == null) {
            return null;
        }
        return category.id.eq(categoryId);
    }

    private BooleanExpression andOption(List<Long> optionIds) {

        if(CollectionUtils.isEmpty(optionIds)) {
            return null;
        }

        return option.id.in(optionIds);
    }

    private BooleanExpression likeProductName(String keyword){
        if(keyword == null || keyword.isEmpty()) {
            return null;
        }
        return product.name.like("%"+keyword+"%");
    }


    private BooleanExpression likeOptionName(String keyword){
        if(keyword == null || keyword.isEmpty()) {
            return null;
        }
        return option.optionName.like("%"+keyword+"%");
    }

    private BooleanExpression likeCategoryName(String keyword){
        if(keyword == null || keyword.isEmpty()) {
            return null;
        }
        return category.categoryName.stringValue().like("%"+keyword+"%");
    }


}
