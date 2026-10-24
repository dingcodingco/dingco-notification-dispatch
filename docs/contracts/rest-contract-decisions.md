# 공개 REST 계약 결정

기준 명세: `openapi/notification-api.yaml` (검사: `npx @redocly/cli lint openapi/notification-api.yaml`). 구현: `CampaignController.java`, `CampaignService.java`.

## 공개 REST 3개
| 메서드·경로 | 목적 | 응답 필드 | 대응 요구사항 |
|---|---|---|---|
| `POST /campaigns` → `201` | 캠페인 초안 생성, 업로드 정보 발급 | `campaignId`, `status`(DRAFT), `uploadUrl`, `objectKey`, `expiresAt` | R1 |
| `POST /campaigns/{id}/dispatch` → `202` | 발송 시작 접수 | `dispatchJobId`, `status`(QUEUED) | R1 |
| `GET /campaigns/{id}` → `200` | 상태·집계 조회 | `status`, `totalRecipients`, `pendingCount`, `sentCount`, `failedCount`, `dlqCount`, `lastUpdatedAt` (+`campaignId`) | R2 |

- dispatch 는 처리 결과가 아니라 접수 결과라서 `202 Accepted` 를 쓴다.
- 모든 요청에 `X-Trace-Id` 를 받을 수 있고, 없으면 서버가 만들어 응답 헤더로 돌려준다.

## 업로드 정보 결정
**문제**: 수신자 파일을 공개 API 를 늘리지 않고 받는다.

| 선택지 | 장점 | 단점 |
|---|---|---|
| multipart 로 Spring 이 직접 받기 | 호출 1번 | 대용량 바이트가 요청 스레드를 지난다. Section 2 의 병목이 업로드 쪽에 다시 생긴다 |
| 별도 upload URL 발급 API | 만료 후 재발급 가능 | 공개 계약이 4개가 된다 |
| **`POST /campaigns` 응답에 presigned PUT 정보** (채택) | 계약 3개 유지, 파일은 S3 로 바로 감, 캠페인과 파일이 생성 시점에 묶임 | 재발급 경로 없음 |

| 필드 | 의미·형식 |
|---|---|
| `uploadUrl` | `lab-recipients` 버킷에 대한 presigned PUT URL. `Content-Type: text/csv` 로 올린다 |
| `objectKey` | `recipients/{campaignId}.csv`. dispatch 때 이 키의 존재를 HeadObject 로 확인한다 |
| `expiresAt` | 업로드 URL 만료 시각(ISO-8601). 생성 시각 + `lab.upload-url-ttl-minutes`(15분) |

**남는 위험**: 만료 뒤 재발급 경로가 공개 계약에 없다. 이번 범위의 대응은 새 캠페인을 만드는 것이다.

## 409 두 경우
| 경우 | 판정 방법 |
|---|---|
| 수신자 파일 미업로드 | `objectKey` HeadObject 가 404 |
| 이미 발송을 시작함 | 상태 갱신 조건 `#s = :draft` 실패(`ConditionalCheckFailedException`) |

조건부 갱신이라서 같은 캠페인에 dispatch 가 동시에 두 번 와도 계획 메시지는 1건만 나간다. 없는 캠페인은 `404`.

## 공개 계약 밖
- `POST /baseline/sync-dispatch`: Section 2 비교용 동기 경로. 명세에 넣지 않는다.
- 비목표: 재시도·취소·별도 업로드 API.

## 알려진 한계(현재 구현)
- `failedCount` 는 항상 0 이다. 실패 집계 경로가 아직 없다.
- `dlqCount` 는 발송 DLQ 전체의 근사 메시지 수이고 캠페인별 값이 아니다(명세 description 에 적음).
