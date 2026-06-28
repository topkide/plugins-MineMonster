# MineMonster (도토리마을)

블록을 캐면 `config.yml`에 등록된 몬스터 중 하나가 **랜덤으로 스폰**되는 Paper 1.21.8 플러그인.

## 빌드
프로젝트 루트에서:
```
./gradlew build
```
결과 JAR: `build/libs/MineMonster-1.0.0.jar`
(인터넷이 막힌 환경에서는 PaperMC 저장소 접근이 필요하므로 일반 PC에서 빌드하세요.)

## 설치
1. 위 JAR을 서버 `plugins/` 폴더에 넣기
2. 서버 재시작
3. `plugins/MineMonster/config.yml`에서 몬스터 목록·확률 수정 후 `/minemonster reload`

## 명령어 (OP 전용)
| 명령어 | 설명 |
| --- | --- |
| `/minemonster on` | 시스템 켜기 |
| `/minemonster off` | 시스템 끄기 |
| `/minemonster reload` | config.yml 새로고침 |

권한: `minemonster.admin` (기본 OP)

## 동작 메모
- OP가 접속하면 제작자 안내 메시지가 개인 메시지로 표시됩니다.
- 워든/엔더드래곤/엘더가디언/위더/파괴수 등 보스는 config에 넣어도 코드에서 자동 제외됩니다.
- 스폰은 블록이 제거된 다음 틱에 처리해 질식·위치 꼬임을 방지합니다.
- `spawn-chance`로 확률(0~100), `max-mobs-per-break`로 마리 수를 조절할 수 있습니다.
