# Nori Settings 설명

## 파일 구조

### 1. Filter (필터) - 동의어 처리
- **nori_synonym**: 동의어 필터
  - `type: "synonym"`: 동의어 필터 타입
  - `synonyms_path`: 동의어 파일 경로 (`elasticsearch/synonyms.txt`)
  - `expand: true`: 확장 모드 (모든 동의어를 인덱싱)

### 2. Analyzer (분석기) - 텍스트 분석
- **nori**: 커스텀 한국어 분석기
  - `type: "custom"`: 커스텀 분석기
  - `tokenizer: "nori_tokenizer"`: Nori 토크나이저 사용
  - `decompound_mode: "mixed"`: 복합어 분해 모드 (원형 + 분해형 모두 유지)
  - `filter`: 적용 순서
    1. `nori_synonym`: 동의어 처리
    2. `lowercase`: 소문자 변환

### 3. Tokenizer (토크나이저) - 텍스트 분리
- **nori_tokenizer**: 한국어 형태소 분석 토크나이저
  - `type: "nori_tokenizer"`: Nori 토크나이저
  - `decompound_mode: "mixed"`: 복합어 분해 모드

## 처리 흐름 예시

입력: "검정색 옵션"

1. **Tokenizer**: "검정색 옵션" → ["검정색", "옵션"]
2. **Synonym Filter**: "검정색" → ["검정색", "black", "검은색", "검정"]
3. **Lowercase Filter**: 모든 토큰 소문자 변환
4. **결과**: ["검정색", "black", "검은색", "검정", "옵션"]

## 사용 위치

`ProductDocument`의 `@Field` 어노테이션에서 사용:
```java
@Field(type = FieldType.Text, analyzer = "nori", searchAnalyzer = "nori")
private String optionNames;
```

## 주의사항

⚠️ **인덱스 재생성 필요**: 설정 변경 후에는 기존 인덱스를 삭제하고 재생성해야 합니다.

