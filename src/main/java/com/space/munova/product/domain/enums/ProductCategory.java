package com.space.munova.product.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProductCategory {
    // 사람 카테고리 (레벨 1)
    MEN("남성"),
    WOMEN("여성"),
    CHILDREN("아동"),
    ALL_PERSON("남여공용"),

    // 신발의 카테고리 (레벨 2)
    SLIPPERS("슬리퍼"),   // 슬리퍼
    SANDALS("샌들"),    // 샌들
    SNEAKERS("스니커즈"),   // 스니커즈
    RUNNING_SHOES("운동화"), // 운동화
    LOAFERS("로퍼"),
    HIGH_HEELS("하이힐"),
    FLAT_SHOES("플랫슈즈"),
    BOOTS("부츠"),
    WALKERS("워커"),
    SLIP_ON("슬립온"),
    CHELSEA_BOOTS("첼시부츠"),
    OXFORD_SHOES("옥스퍼드 슈즈"),
    WINTER_BOOTS("방한화"),    // 방한화
    RAIN_BOOTS("레인부츠"),    // 레인부츠
    AQUA_SHOES("아쿠아슈즈"), // 아쿠아슈즈
    DRESS_SHOES("드레스 신발");  // 드레스 신발

    private final String description;
}
