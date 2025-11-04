// 생성됨 - 2025-10-14 23:30
package com.kkc.discord_bot.constant;

/**
 * Discord 봇의 전역 상수를 관리하는 클래스
 * 매직 넘버와 하드코딩된 문자열을 중앙화하여 관리합니다.
 */
public final class BotConstants {
    
    // ========== 타이머 관련 상수 ==========
    /**
     * 음악이 없을 때 자동 퇴장까지의 대기 시간 (초)
     */
    public static final int DISCONNECT_TIMEOUT_SECONDS = 30;
    
    /**
     * 음성 채널에 혼자 있을 때 자동 퇴장까지의 대기 시간 (초)
     */
    public static final int ALONE_DISCONNECT_TIMEOUT_SECONDS = 30;
    
    /**
     * 검색 결과 자동 삭제 시간 (초)
     */
    public static final int SEARCH_RESULT_TIMEOUT_SECONDS = 30;
    
    // ========== 재생목록 관련 상수 ==========
    /**
     * 재생목록 표시 시 최대 트랙 개수
     */
    public static final int MAX_QUEUE_DISPLAY_LIMIT = 10;
    
    /**
     * JDA Embed 필드의 최대 문자 수 제한
     */
    public static final int EMBED_FIELD_MAX_CHARACTERS = 1024;
    
    /**
     * 검색 결과 최대 표시 개수
     */
    public static final int SEARCH_RESULT_LIMIT = 5;
    
    /**
     * 비밀번호 없이 재생할 때 랜덤 선택 곡 수
     */
    public static final int RANDOM_PLAY_TRACK_COUNT = 10;
    
    /**
     * 저장된 음악 목록 조회 시 최대 표시 개수
     */
    public static final int SAVED_MUSIC_LIST_DISPLAY_LIMIT = 20;
    
    // ========== 오디오 품질 관련 상수 ==========
    /**
     * Opus 인코딩 품질 (최대값)
     */
    public static final int OPUS_ENCODING_QUALITY_MAX = 10;
    
    // ========== 메시지 상수 ==========
    /**
     * 음성 채널 미참여 경고 메시지
     */
    public static final String MSG_NOT_IN_VOICE_CHANNEL = "❌ 소속해 있는 보이스 채널이 없습니다.";
    
    /**
     * 재생 중인 음악 없음 메시지
     */
    public static final String MSG_NO_MUSIC_PLAYING = "❌ 현재 재생 중인 음악이 없습니다.";
    
    /**
     * 대기열 비어있음 메시지
     */
    public static final String MSG_QUEUE_EMPTY = "⚠️ 스킵할 음악이 없습니다. 대기열이 비어 있습니다.";
    
    /**
     * 검색 결과 없음 메시지
     */
    public static final String MSG_NO_SEARCH_RESULTS = "❌ 검색 결과가 없습니다.";
    
    /**
     * 음악 정지 완료 메시지
     */
    public static final String MSG_MUSIC_STOPPED = "🛑 음악 재생을 중지하고 모든 트랙을 삭제했습니다.";
    
    /**
     * 재생목록 셔플 완료 메시지
     */
    public static final String MSG_QUEUE_SHUFFLED = "🔀 **재생목록을 랜덤으로 섞었습니다!**";
    
    // ========== 유틸리티 메서드 ==========
    private BotConstants() {
        // 인스턴스화 방지
        throw new AssertionError("Constants class cannot be instantiated");
    }
}
