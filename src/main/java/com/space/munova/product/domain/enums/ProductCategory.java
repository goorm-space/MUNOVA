package com.space.munova.product.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public enum ProductCategory {

    MEN(null, "남성", 1),
    WOMEN(null, "여성", 1),
    CHILDREN(null, "아동", 1),
    ALL_PERSON(null, "남여공용", 1),

    // --- 남성용 (MEN: M_) ---
    M_SLIPPERS(MEN, "슬리퍼", 2),
    M_SANDALS(MEN, "샌들", 2),
    M_SNEAKERS(MEN, "스니커즈", 2),
    M_RUNNING_SHOES(MEN, "운동화", 2),
    M_LOAFERS(MEN, "로퍼", 2),
    M_BOOTS(MEN, "부츠", 2),
    M_WALKERS(MEN, "워커", 2),
    M_SLIP_ON(MEN, "슬립온", 2),
    M_CHELSEA_BOOTS(MEN, "첼시부츠", 2),
    M_OXFORD_SHOES(MEN, "옥스퍼드 슈즈", 2),
    M_WINTER_BOOTS(MEN, "방한화", 2),
    M_RAIN_BOOTS(MEN, "레인부츠", 2),
    M_AQUA_SHOES(MEN, "아쿠아슈즈", 2),
    M_DRESS_SHOES(MEN, "드레스 신발", 2),

    // --- 여성용 (WOMEN: W_) ---
    W_HIGH_HEELS(WOMEN, "하이힐", 2),
    W_FLAT_SHOES(WOMEN, "플랫슈즈", 2),
    W_SLIPPERS(WOMEN, "슬리퍼", 2),
    W_SANDALS(WOMEN, "샌들", 2),
    W_SNEAKERS(WOMEN, "스니커즈", 2),
    W_RUNNING_SHOES(WOMEN, "운동화", 2),
    W_LOAFERS(WOMEN, "로퍼", 2),
    W_BOOTS(WOMEN, "부츠", 2),
    W_WALKERS(WOMEN, "워커", 2),
    W_SLIP_ON(WOMEN, "슬립온", 2),
    W_CHELSEA_BOOTS(WOMEN, "첼시부츠", 2),
    W_OXFORD_SHOES(WOMEN, "옥스퍼드 슈즈", 2),
    W_WINTER_BOOTS(WOMEN, "방한화", 2),
    W_RAIN_BOOTS(WOMEN, "레인부츠", 2),
    W_AQUA_SHOES(WOMEN, "아쿠아슈즈", 2),
    W_DRESS_SHOES(WOMEN, "드레스 신발", 2),

    // --- 아동용 (CHILDREN: C_) ---
    C_SLIPPERS(CHILDREN, "슬리퍼", 2),
    C_SANDALS(CHILDREN, "샌들", 2),
    C_SNEAKERS(CHILDREN, "스니커즈", 2),
    C_RUNNING_SHOES(CHILDREN, "운동화", 2),
    C_LOAFERS(CHILDREN, "로퍼", 2),
    C_BOOTS(CHILDREN, "부츠", 2),
    C_WALKERS(CHILDREN, "워커", 2),
    C_SLIP_ON(CHILDREN, "슬립온", 2),
    C_WINTER_BOOTS(CHILDREN, "방한화", 2),
    C_RAIN_BOOTS(CHILDREN, "레인부츠", 2),
    C_AQUA_SHOES(CHILDREN, "아쿠아슈즈", 2),

    // --- 남여공용 (ALL_PERSON: A_) ---
    A_SLIPPERS(ALL_PERSON, "슬리퍼", 2),
    A_SANDALS(ALL_PERSON, "샌들", 2),
    A_SNEAKERS(ALL_PERSON, "스니커즈", 2),
    A_RUNNING_SHOES(ALL_PERSON, "운동화", 2),
    A_LOAFERS(ALL_PERSON, "로퍼", 2),
    A_BOOTS(ALL_PERSON, "부츠", 2),
    A_WALKERS(ALL_PERSON, "워커", 2),
    A_SLIP_ON(ALL_PERSON, "슬립온", 2),
    A_WINTER_BOOTS(ALL_PERSON, "방한화", 2),
    A_RAIN_BOOTS(ALL_PERSON, "레인부츠", 2),
    A_AQUA_SHOES(ALL_PERSON, "아쿠아슈즈", 2);


    private final ProductCategory parentCategory;
    private final String description;
    private final int level;

    /**
     * 특정 레벨의 카테고리 목록을 찾습니다. (예: 1레벨 카테고리 목록)
     */
    public static List<ProductCategory> findByLevel(int level) {
        return Arrays.stream(ProductCategory.values())
                .filter(c -> c.getLevel() == level)
                .collect(Collectors.toList());
    }

    /**
     * 특정 부모 카테고리의 자식 목록을 찾습니다.
     */
    public static List<ProductCategory> findChildrenOf(ProductCategory parent) {
        if (parent == null) {
            return List.of();
        }
        return Arrays.stream(ProductCategory.values())
                .filter(c -> parent.equals(c.getParentCategory()))
                .collect(Collectors.toList());
    }


}
