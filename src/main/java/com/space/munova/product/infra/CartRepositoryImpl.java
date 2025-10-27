package com.space.munova.product.infra;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.space.munova.product.application.dto.cart.ProductInfoForCartDto;
import com.space.munova.product.domain.*;
import com.space.munova.product.domain.Repository.CartRepositoryCustom;
import com.space.munova.product.domain.enums.ProductImageType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.space.munova.product.domain.QBrand.brand;
import static com.space.munova.product.domain.QCart.cart;
import static com.space.munova.product.domain.QOption.option;
import static com.space.munova.product.domain.QProduct.product;
import static com.space.munova.product.domain.QProductDetail.productDetail;
import static com.space.munova.product.domain.QProductImage.productImage;
import static com.space.munova.product.domain.QProductOptionMapping.productOptionMapping;

@Repository
@RequiredArgsConstructor
public class CartRepositoryImpl implements CartRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ProductInfoForCartDto> findCartItemInfoByMemberId(Long memberId, Pageable pageable) {

        return queryFactory
                .select(Projections.constructor(ProductInfoForCartDto.class,
                        product.id.as("productId"),
                        cart.id.as("cartId"),
                        productDetail.id.as("detailId"),
                        product.name.as("productName"),
                        product.price.as("productPrice"),
                        productDetail.quantity.as("productQuantity"),
                        cart.quantity.as("cartItemQuantity"),
                        productImage.savedName.as("mainImgSrc"),
                        brand.brandName.as("brandName"),
                        option.id.as("optionId"),
                        option.optionType.as("optionType"),
                        option.optionName.as("optionName")
                        ))
                .from(cart)
                .leftJoin(productDetail)
                .on(cart.productDetail.id.eq(productDetail.id))
                .leftJoin(product)
                .on(product.id.eq(productDetail.product.id))
                .leftJoin(productImage)
                .on(productImage.product.id.eq(product.id)
                        .and(productImage.imageType.eq(ProductImageType.MAIN)))
                .leftJoin(brand)
                .on(product.brand.id.eq(brand.id))
                .leftJoin(productOptionMapping)
                .on(productDetail.id.eq(productOptionMapping.productDetail.id))
                .leftJoin(option)
                .on(productOptionMapping.option.id.eq(option.id))
                .where(cart.member.id.eq(memberId)
                        .and(cart.isDeleted.eq(false)))
                .orderBy(cart.createdAt.desc())
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetch();
    }




}
