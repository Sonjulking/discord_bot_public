// 생성됨 - 2025-10-14 23:31
package com.kkc.discord_bot.constant;

/**
 * Discord 봇의 메시지 템플릿을 관리하는 클래스
 * 이모지와 메시지 포맷을 중앙화하여 일관성을 유지합니다.
 */
public final class MessageConstants {
    
    // ========== 이모지 상수 ==========
    public static final String EMOJI_MUSIC = "🎵";
    public static final String EMOJI_STOP = "🛑";
    public static final String EMOJI_NEXT = "⏭️";
    public static final String EMOJI_PREV = "⏮️";
    public static final String EMOJI_SHUFFLE = "🔀";
    public static final String EMOJI_ERROR = "❌";
    public static final String EMOJI_WARNING = "⚠️";
    public static final String EMOJI_SUCCESS = "✅";
    public static final String EMOJI_SEARCH = "🔍";
    public static final String EMOJI_LOCK = "🔐";
    public static final String EMOJI_LIST = "📜";
    public static final String EMOJI_PLAY = "▶";
    public static final String EMOJI_WAIT = "⏳";
    public static final String EMOJI_WAVE = "👋";
    public static final String EMOJI_MUTE = "🔇";
    
    // ========== 음악 재생 관련 메시지 ==========
    public static final String MSG_TRACK_QUEUED = EMOJI_MUSIC + " 등록된 곡: `%s` (by `%s`)";
    public static final String MSG_NOW_PLAYING = EMOJI_MUSIC + " **현재 재생 중:** %s";
    public static final String MSG_NO_TRACKS = EMOJI_LIST + EMOJI_MUSIC + "등록된 노래가 없습니다.";
    public static final String MSG_PLAYLIST_ADDED = EMOJI_LIST + " 유튜브 재생목록 추가됨: `%s` (%d곡)";
    public static final String MSG_RANDOM_PLAY_ENABLED = EMOJI_SHUFFLE + " **재생목록이 랜덤으로 재생됩니다!**";
    
    // ========== 에러 메시지 ==========
    public static final String MSG_NOT_IN_VOICE_CHANNEL = EMOJI_ERROR + " 소속해 있는 보이스 채널이 없습니다.";
    public static final String MSG_NO_MUSIC_PLAYING = EMOJI_ERROR + " 현재 재생 중인 음악이 없습니다.";
    public static final String MSG_NO_MATCHES = EMOJI_WARNING + " 일치하는 결과가 없습니다. %s";
    public static final String MSG_LOAD_FAILED = EMOJI_ERROR + " 재생할 수 없습니다. %s";
    public static final String MSG_QUEUE_EMPTY = EMOJI_WARNING + " 스킵할 음악이 없습니다. 대기열이 비어 있습니다.";
    public static final String MSG_NO_PREVIOUS_TRACK = EMOJI_PREV + " **이전 곡이 없습니다!**";
    public static final String MSG_SHUFFLE_FAILED = EMOJI_WARNING + " 대기열에 곡이 없어서 셔플할 수 없습니다.";
    
    // ========== 제어 메시지 ==========
    public static final String MSG_MUSIC_STOPPED = EMOJI_STOP + " 음악 재생을 중지하고 모든 트랙을 삭제했습니다.";
    public static final String MSG_QUEUE_SHUFFLED = EMOJI_SHUFFLE + " **재생목록을 랜덤으로 섞었습니다!**";
    public static final String MSG_NEXT_TRACK = EMOJI_NEXT + " 다음 트랙으로 넘어갑니다.";
    public static final String MSG_LAST_TRACK = EMOJI_MUSIC + " 마지막 곡입니다. 더 이상 스킵할 트랙이 없습니다. " + EMOJI_MUSIC;
    
    // ========== 자동 퇴장 메시지 ==========
    public static final String MSG_DISCONNECT_NO_MUSIC = EMOJI_MUTE + " 30초 동안 음악이 없어 음성 채널에서 나갑니다.";
    public static final String MSG_DISCONNECT_ALONE = EMOJI_WAVE + " 30초 동안 음성 채널에 사람이 없어서 나갑니다.";
    
    // ========== 저장 관련 메시지 ==========
    public static final String MSG_TRACK_SAVED = EMOJI_SUCCESS + " **%s** 음악이 비밀번호 '%s'로 등록되었습니다.";
    public static final String MSG_PLAYLIST_SAVING = EMOJI_WAIT + " **플레이리스트 저장 중...** (총 %d곡)\n" + 
                                                      EMOJI_WARNING + " YouTube API 제한으로 최대 100곡까지만 저장됩니다.";
    public static final String MSG_PLAYLIST_SAVED = EMOJI_SUCCESS + " **플레이리스트 저장 완료!** 총 %d곡이 비밀번호 '%s'로 등록되었습니다.";
    public static final String MSG_PASSWORD_PROMPT = EMOJI_LOCK + " **비밀번호를 입력해주세요:** (취소: `c`)";
    public static final String MSG_SAVE_CANCELLED = EMOJI_ERROR + " **음악 등록이 취소되었습니다.**";
    public static final String MSG_NO_SAVED_MUSIC = EMOJI_ERROR + " 저장된 음악이 없습니다. `!save <URL>`로 음악을 추가해주세요.";
    
    // ========== 검색 관련 메시지 ==========
    public static final String MSG_SEARCH_CANCELLED = EMOJI_ERROR + " **검색이 취소되었습니다.**";
    public static final String MSG_SELECTED_TRACK = EMOJI_PLAY + " **선택한 곡:** %s";
    public static final String MSG_MUSIC_LOADING = EMOJI_MUSIC + " **음악 정보를 불러오는 중...**";
    
    // ========== 도움말 메시지 ==========
    public static final String MSG_HELP = 
            EMOJI_MUSIC + "  !play, !p 제목 또는 링크 :  트랙 재생\n" +
            EMOJI_NEXT + "  !next 또는 !skip : 다음 트랙 \n" +
            EMOJI_STOP + "  !stop :  음악 전체중지(가운데 손가락 아님) \n" +
            EMOJI_LIST + " !list : 재생목록 \n" +
            EMOJI_SHUFFLE + " !shuffle : 재생목록 섞기";
    
    private MessageConstants() {
        // 인스턴스화 방지
        throw new AssertionError("Message constants class cannot be instantiated");
    }
}
