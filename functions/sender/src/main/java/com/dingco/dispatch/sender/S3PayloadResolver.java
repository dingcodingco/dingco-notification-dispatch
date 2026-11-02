package com.dingco.dispatch.sender;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class S3PayloadResolver implements PayloadResolver {
    private final S3Client s3;

    public S3PayloadResolver(S3Client s3) {
        this.s3 = s3;
    }

    @Override
    public void requireExists(String payloadRef) {
        String path = payloadRef.substring("s3://".length());
        int slash = path.indexOf('/');
        String bucket = path.substring(0, slash);
        String key = path.substring(slash + 1);
        try {
            s3.headObject(b -> b.bucket(bucket).key(key));
        } catch (NoSuchKeyException e) {
            throw new PoisonMessageException("payloadRef 해석 실패(객체 없음): " + payloadRef);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new PoisonMessageException("payloadRef 해석 실패(객체 없음): " + payloadRef);
            }
            throw e;
        }
    }
}
