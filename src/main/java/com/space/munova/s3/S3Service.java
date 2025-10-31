package com.space.munova.s3;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${spring.cloud.aws.bucket}")
    private String bucket;

    // 업로드 할 file 를 받아서 처리 ->
    public String uploadFile(MultipartFile file) throws IOException {
        // UUID -> 고유 ID + 원본 사진 => 고유 사진 파일 이름 생성
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // 업로드하는 파일은 문서(pdf)나 이미지만 허용되도록
        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("application/pdf") &&
                        !contentType.startsWith("image/jpeg") &&
                        !contentType.startsWith("image/png"))) {
            throw S3Exception.throwUnsupportedFileTypeException("contentType : ", contentType);
        }

        // 파일의 메타데이터 설정
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());    // 업로드 되는 파일의 MIME 타입 지정 ex) file.jpg, file.png
        metadata.setContentLength(file.getSize());     // 업로드 파일의 크기를 바이트 단위로 설정

        // S3에 파일 업로드 요청 생성
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, fileName, file.getInputStream(), metadata);
        // S3에 파일 업로드
        amazonS3.putObject(putObjectRequest);

        // 파일이 성공적으로 업로드된 경우 -> https://munova-product-img-bucket.s3.ap-northeast-2.amazonaws.com/<파일명> 의 이름으로 URL 생성
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, amazonS3.getRegionName(), fileName);
    }


    // 여러 파일 업로드
    public List<String> uploadFiles(List<MultipartFile> files) throws IOException {


        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile file : files) {

            if (file != null && !file.isEmpty()) {

                String url = uploadFile(file);
                uploadedUrls.add(url);
            }
        }

        return uploadedUrls;
    }

    // 파일 삭제
    public void deleteFile(String fileUrl) {

        String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        log.info(filename);

        if (!amazonS3.doesObjectExist(bucket, filename)) {
            throw S3Exception.fileNotFoundException("filename : ", filename);
        }
        amazonS3.deleteObject(bucket, filename);
    }


    // 여러파일 삭제
    public void deleteFiles(List<String> fileUrls) {

        // 전체 URL 목록을 파일 이름 목록으로 변환
        List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>();
        for (String fileUrl : fileUrls) {
            String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            keys.add(new DeleteObjectsRequest.KeyVersion(filename));
        }

        // 삭제할 파일 목록이 없으면 아무것도 하지 않음
        if (keys.isEmpty()) {
            return;
        }

        // 배치 삭제 요청 객체 생성
        DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucket)
                .withKeys(keys);

        // 삭제를 요청 호출 1회
        DeleteObjectsResult result = amazonS3.deleteObjects(deleteObjectsRequest);
    }
}
