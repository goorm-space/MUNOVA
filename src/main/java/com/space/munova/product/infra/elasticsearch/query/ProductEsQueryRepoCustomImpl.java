package com.space.munova.product.infra.elasticsearch.query;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.space.munova.product.application.product.query.dto.FindProductResponseDto;
import com.space.munova.product.application.product.query.dto.ProductCursorDto;
import com.space.munova.product.domain.enums.SortFlag;
import com.space.munova.product.infra.elasticsearch.ProductEsDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Repository;
import org.springframework.data.elasticsearch.core.query.SourceFilter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Repository
@RequiredArgsConstructor
public class ProductEsQueryRepoCustomImpl implements ProductEsQueryRepoCustom {


    private final ElasticsearchOperations esOperations;

    @Override
    public Page<FindProductResponseDto> search(SortFlag sortFlag,
                                               ProductCursorDto cursor,
                                               Long categoryId,
                                               List<Long> optionIds,
                                               String keyword,
                                               Pageable pageable) {

        List<FindProductResponseDto> values =
                getFindProductResponseDto(sortFlag, cursor, categoryId, optionIds, keyword, pageable);

        boolean hasNext = values.size() > pageable.getPageSize();

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


    /// ----------------------------- 내부 로직 ----------------------------------------------------///

    private void andKeywords(String keyword, Long categoryId, List<Long> optionIds, BoolQuery.Builder bBuilder) {
        if (keyword != null && !keyword.isEmpty()) {
            // 필드 리스트 동적 생성
            List<String> fields = new ArrayList<>();
            fields.add("name^3");
            fields.add("brandName^2");

            // categoryId가 없을 때만 categoryName 포함 (이미 필터링됐으면 제외)
            if (categoryId == null) {
                fields.add("categoryName^1");
            }

            // optionIds가 없을 때만 optionNames 포함 (이미 필터링됐으면 제외)
            if (optionIds == null || optionIds.isEmpty()) {
                fields.add("optionNames^2");
            }

            bBuilder.must(Query.of(q ->
                    q.multiMatch(m ->
                            m.query(keyword)
                                    .fields(fields)
                                    .type(TextQueryType.BestFields)
                    )

            ));
        }
    }

    private void andOptions(List<Long> optionIds, BoolQuery.Builder bBuilder) {
        if (optionIds != null && !optionIds.isEmpty()) {

            bBuilder.must(Query.of(q ->
                            q.terms(t ->
                                    t.field("optionIds")
                                            .terms(tms ->
                                                    tms.value(optionIds.stream()
                                                            .map(id -> FieldValue.of(id))
                                                            .toList()
                                                    )
                                            )
                            )
                    )
            );
        }
    }

    private void andCategory(Long categoryId, BoolQuery.Builder bBuilder) {
        if (categoryId != null) {

            bBuilder.must(Query.of(q ->
                            q.term(t ->
                                    t.field("categoryId")
                                            .value(categoryId)
                            )
                    )
            );
        }
    }


    private void andSearchAfter(SortFlag sortFlag, ProductCursorDto cursor, NativeQueryBuilder nqBuilder) {
        if (cursor != null) {

            List<Object> searchAfterValues = new ArrayList<>();

            Comparable<?> sortValue = cursor.getSortValue(sortFlag);
            if (sortValue != null) {

                if (sortValue instanceof LocalDate) {
                    LocalDate localDate = (LocalDate) sortValue;
                    searchAfterValues.add(localDate.toString());
                } else {
                    searchAfterValues.add(sortValue);
                }
            }

            searchAfterValues.add(cursor.getProductId());
            nqBuilder.withSearchAfter(searchAfterValues);
        }
    }


    private void sortValues(NativeQueryBuilder nqBuilder, BoolQuery.Builder bBuilder, String sortFlagName, int size) {
        nqBuilder.withQuery(q -> q.bool(b -> b.must(bBuilder.build())));

        nqBuilder.withSort(SortOptions.of(s ->
                        s.field(sf ->
                                sf.field(sortFlagName).order(SortOrder.Desc)
                        )
                )
        );

        nqBuilder.withSort(SortOptions.of(s ->
                        s.field(sf ->
                                sf.field("productId").order(SortOrder.Desc)
                        )
                )
        );

        /// limt size
        nqBuilder.withMaxResults(size);
    }


    private void whereConditions(SortFlag sortFlag,
                                 ProductCursorDto cursor,
                                 Long categoryId, List<Long> optionIds,
                                 String keyword,
                                 String sortFlagName,
                                 BoolQuery.Builder bBuilder,
                                 NativeQueryBuilder nqBuilder) {
        andCategory(categoryId, bBuilder);

        /// 옵션필터링
        andOptions(optionIds, bBuilder);

        /// 커서 조건.
        andSearchAfter(sortFlag, cursor, nqBuilder);

        /// 키워드 필터링 (categoryId, optionIds 전달하여 조건부 필드 포함)
        andKeywords(keyword, categoryId, optionIds, bBuilder);
    }


    private List<FindProductResponseDto> getFindProductResponseDto(SortFlag sortFlag, ProductCursorDto cursor, Long categoryId, List<Long> optionIds, String keyword, Pageable pageable) {
        String sortFlagName = sortFlag.getName();


        NativeQueryBuilder nqBuilder = new NativeQueryBuilder();

        BoolQuery.Builder bBuilder = new BoolQuery.Builder();

        SourceFilter sourceFilter = new SourceFilter() {
            @Override
            public String[] getIncludes() {
                return new String[]{"productId", "mainImageUrl", "brandName", "name",
                        "price", "likeCount", "salesCount", "viewCount", "createdAt"};
            }

            @Override
            public String[] getExcludes() {
                return new String[0];
            }
        };

        nqBuilder.withSourceFilter(sourceFilter);
        /// 조건 순서 -> 카테고리 -> 옵션 -> 커서 -> 키워드
        /// 카테고리 필터링
        whereConditions(sortFlag,
                cursor,
                categoryId,
                optionIds,
                keyword,
                sortFlagName,
                bBuilder,
                nqBuilder);

        /// 소팅
        sortValues(nqBuilder, bBuilder, sortFlagName, pageable.getPageSize() + 1);

        List<FindProductResponseDto> values = esOperations
                .search(nqBuilder.build(), ProductEsDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .map(FindProductResponseDto::from)
                .toList();
        return values;
    }

}
