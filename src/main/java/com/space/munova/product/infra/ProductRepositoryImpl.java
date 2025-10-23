package com.space.munova.product.infra;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.domain.Repository.ProductRepositoryCustom;
import com.space.munova.product.domain.enums.ProductImageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public List<FindProductResponseDto> findProductByConditions(Long categoryId, List<Long> optionIds, String keyword, Pageable pageable) {

        List<FindProductResponseDto> findProductByConditions = queryFactory
                .select(Projections.constructor(FindProductResponseDto.class,
                        product.id.as("productId"),
                        productImage.originName.as("mainImgSrc"),
                        brand.brandName.as("brandName"),
                        product.name.as("productName"),
                        product.price.as("price")
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
                        likeProductName(keyword)
                        )
                .distinct()
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetch();

        return findProductByConditions;
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
}
