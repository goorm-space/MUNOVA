package com.space.munova.recommend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseDto {
    private Long id;
    private String name;
    private String info;
    private Long price;
    private Integer likeCount;
    private Integer salesCount;
    private Integer viewCount;
    private BrandDto brand;
    private CategoryDto category;
    private MemberDto member;
    private boolean isDeleted;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BrandDto {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryDto {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberDto {
        private Long id;
        private String username; // Member 엔티티에 맞춰 필요한 필드만 포함
    }
}