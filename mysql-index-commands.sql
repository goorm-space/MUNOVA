-- MySQL 인덱스 생성 명령어
-- 사용법: docker exec -i munova-mysql mysql -uroot -p1234 munova_db < mysql-index-commands.sql
-- 또는 MySQL 클라이언트에서 직접 실행

USE munova_db;

-- ============================================
-- 1. product_search_log 테이블 인덱스
-- ============================================
-- member_id 인덱스
CREATE INDEX IF NOT EXISTS idx_user_search_member_id ON product_search_log(member_id);

-- search_category_id 인덱스
CREATE INDEX IF NOT EXISTS idx_user_search_category_id ON product_search_log(search_category_id);

-- created_at 인덱스
CREATE INDEX IF NOT EXISTS idx_user_search_created_at ON product_search_log(created_at);

-- ============================================
-- 2. user_recommendations 테이블 인덱스
-- ============================================
-- member_id 인덱스 (엔티티에서는 user_id로 정의되어 있지만 실제 컬럼은 member_id)
CREATE INDEX IF NOT EXISTS idx_userrec_userid ON user_recommendations(member_id);

-- product_id 인덱스 (추가 권장 - 조인 성능 향상)
CREATE INDEX IF NOT EXISTS idx_userrec_productid ON user_recommendations(product_id);

-- ============================================
-- 3. product_recommendations 테이블 인덱스
-- ============================================
-- source_product_id 인덱스
CREATE INDEX IF NOT EXISTS idx_prodrec_sourceid ON product_recommendations(source_product_id);

-- target_product_id 인덱스 (추가 권장 - 조인 성능 향상)
CREATE INDEX IF NOT EXISTS idx_prodrec_targetid ON product_recommendations(target_product_id);

-- ============================================
-- 4. product 테이블 인덱스 (성능 최적화)
-- ============================================
-- brand_id 인덱스 (외래키 조인 성능 향상)
CREATE INDEX IF NOT EXISTS idx_product_brand_id ON product(brand_id);

-- product_category_id 인덱스 (외래키 조인 성능 향상)
CREATE INDEX IF NOT EXISTS idx_product_category_id ON product(product_category_id);

-- member_id 인덱스 (외래키 조인 성능 향상)
CREATE INDEX IF NOT EXISTS idx_product_member_id ON product(member_id);

-- isDeleted 인덱스 (논리 삭제 필터링 성능 향상)
CREATE INDEX IF NOT EXISTS idx_product_is_deleted ON product(is_deleted);

-- 복합 인덱스: isDeleted + categoryId (자주 함께 사용되는 경우)
CREATE INDEX IF NOT EXISTS idx_product_category_deleted ON product(product_category_id, is_deleted);

-- ============================================
-- 5. product_detail 테이블 인덱스 (성능 최적화)
-- ============================================
-- product_id 인덱스 (외래키 조인 성능 향상)
CREATE INDEX IF NOT EXISTS idx_product_detail_product_id ON product_detail(product_id);

-- isDeleted 인덱스 (논리 삭제 필터링 성능 향상)
CREATE INDEX IF NOT EXISTS idx_product_detail_is_deleted ON product_detail(is_deleted);

-- ============================================
-- 6. 인덱스 생성 확인 쿼리
-- ============================================
-- 모든 인덱스 확인
SHOW INDEX FROM product_search_log;
SHOW INDEX FROM user_recommendations;
SHOW INDEX FROM product_recommendations;
SHOW INDEX FROM product;
SHOW INDEX FROM product_detail;

-- 특정 테이블의 인덱스 확인
-- SELECT * FROM information_schema.statistics 
-- WHERE table_schema = 'munova_db' AND table_name = 'product';

