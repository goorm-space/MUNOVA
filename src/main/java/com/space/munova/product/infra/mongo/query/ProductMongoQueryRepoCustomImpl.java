package com.space.munova.product.infra.mongo.query;

import com.space.munova.product.application.product.query.dto.FindProductResponseDto;
import com.space.munova.product.application.product.query.dto.ProductCursorDto;
import com.space.munova.product.domain.enums.SortFlag;
import com.space.munova.product.infra.mongo.ProductMongoDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductMongoQueryRepoCustomImpl implements ProductMongoQueryRepoCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<FindProductResponseDto> findByConditions(SortFlag sortFlag,
                                                         ProductCursorDto cursor,
                                                         Long categoryId,
                                                         List<Long> optionIds,
                                                         Pageable pageable) {


        List<Criteria> ands = new ArrayList<>();

        String sortFlagName = sortFlag.getName();

        if (categoryId != null) {
            ands.add(Criteria.where("categoryId").is(categoryId));
        }
        if (optionIds != null && !optionIds.isEmpty()) {
            ands.add(Criteria.where("optionIds").in(optionIds));
        }



        Criteria resultCriteria = getResultCriteria(sortFlag, cursor, ands, sortFlagName);

        Sort sort = Sort.by(
                Sort.Order.desc(sortFlagName),
                Sort.Order.desc("_id")
        );

        Query query = new Query(resultCriteria)
                .with(sort)
                .limit(pageable.getPageSize() + 1);  // hasNext 확인을 위해 1개 더 조회

        query.fields()
                .include("productId")
                .include("name")
                .include("brandName")
                .include("price")
                .include("likeCount")
                .include("salesCount")
                .include("viewCount")
                .include("createdAt")
                .include("mainImage");  // 중첩 객체 전체 포함 (mainImage.imageUrl 접근을 위해)

        // ProductMongoDocument로 조회 후 FindProductResponseDto로 변환
        List<ProductMongoDocument> documents = mongoTemplate.find(query, ProductMongoDocument.class, "product");
        List<FindProductResponseDto> values = documents.stream()
                .map(FindProductResponseDto::from)
                .toList();


        boolean hasNext = values.size() > pageable.getPageSize();

        /// 다음 페이지가 있다면 요청한 사이즈만큼만 반환 (마지막 1개 제외)
        if (hasNext) {
            values = values.subList(0, pageable.getPageSize());
        }

        return new PageImpl<>(values, pageable, -1L) {
            @Override
            public boolean hasNext() {

                return hasNext;
            }

            @Override
            public boolean isLast() {

                return !hasNext();
            }
        };
    }


    private Criteria getResultCriteria(SortFlag sortFlag, ProductCursorDto cursor, List<Criteria> ands, String sortFlagName) {

        Criteria resultCriteria;

        if (cursor != null) {
            List<Criteria> conditionOne = new ArrayList<>();
            List<Criteria> conditionTwo = new ArrayList<>();
            /// 정렬조건 가져옴.
            Comparable<?> sortValue = cursor.getSortValue(sortFlag);
            conditionOne.addAll(ands);
            conditionOne.add(Criteria.where(sortFlagName).lt(sortValue));


            conditionTwo.addAll(ands);
            conditionTwo.add(Criteria.where(sortFlagName).is(sortValue));
            conditionTwo.add(Criteria.where("_id").lt(cursor.getProductId()));

            Criteria criteriaOne = new Criteria()
                    .andOperator(conditionOne.toArray(new Criteria[conditionOne.size()]));
            Criteria criteriaTwo = new Criteria()
                    .andOperator(conditionTwo.toArray(new Criteria[conditionTwo.size()]));

            resultCriteria = new Criteria().orOperator(
                    criteriaOne,
                    criteriaTwo
            );

        } else {
            /// 첫페이지 일경우
            List<Criteria> defaultCondition = new ArrayList<>();
            defaultCondition.addAll(ands);

            resultCriteria = ands.isEmpty() ? new Criteria()
                    : new Criteria().andOperator(defaultCondition.toArray(new Criteria[defaultCondition.size()]));

        }
        return resultCriteria;
    }


}