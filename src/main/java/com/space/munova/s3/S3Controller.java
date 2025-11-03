package com.space.munova.s3;

import com.space.munova.core.config.ResponseApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseApi<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = s3Service.uploadFile(file);
            return ResponseApi.ok(imageUrl);
        } catch (S3Exception e) {
            // S3Exception이 이미 code, message, HttpStatus를 가지고 있음
            return ResponseApi.nok(e.getStatusCode(), e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("파일 업로드 실패", e);
            return ResponseApi.nok(HttpStatus.INTERNAL_SERVER_ERROR, "S3_99", "파일 업로드 중 알 수 없는 오류가 발생했습니다.");
        }
    }

    @PostMapping("/delete")
    public ResponseApi<String> delete(@RequestParam String filename) {
        try {
            s3Service.deleteFile(filename);
            return ResponseApi.ok("파일 삭제에 성공했습니다.");
        } catch (S3Exception e) {
            return ResponseApi.nok(e.getStatusCode(), e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("파일 삭제 실패", e);
            return ResponseApi.nok(HttpStatus.INTERNAL_SERVER_ERROR, "S3_99", "파일 삭제 중 알 수 없는 오류가 발생했습니다.");
        }
    }
}
