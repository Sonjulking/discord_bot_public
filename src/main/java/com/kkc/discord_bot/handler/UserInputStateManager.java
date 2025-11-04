// 생성됨 - 2025-11-03
package com.kkc.discord_bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 사용자 입력 상태 관리자
 *
 * 사용자의 입력 대기 상태를 관리합니다.
 * - 검색 결과 선택 대기
 * - 랜덤 재생 선택 대기
 * - 플레이리스트 이름 입력 대기
 * - 저장된 플레이리스트 랜덤 재생 선택 대기
 *
 * @author KKC
 * @since 2025-11-03
 */
@Slf4j
@Component
public class UserInputStateManager {

    /** 검색 결과 저장소 (사용자 ID -> 검색 결과 리스트) */
    private final ConcurrentHashMap<String, Object> searchResults = new ConcurrentHashMap<>();

    /** 랜덤 재생 선택 대기 저장소 (사용자 ID -> 플레이리스트 URL) */
    private final ConcurrentHashMap<String, String> pendingRandomChoice = new ConcurrentHashMap<>();

    /** 플레이리스트 이름 입력 대기 저장소 (사용자 ID -> 트랙 URL) */
    private final ConcurrentHashMap<String, String> pendingPlaylistNameInput = new ConcurrentHashMap<>();

    /** 저장된 플레이리스트 랜덤 재생 선택 대기 저장소 (사용자 ID -> 플레이리스트 이름) */
    private final ConcurrentHashMap<String, String> pendingSavedPlaylistRandomChoice = new ConcurrentHashMap<>();

    /** 스케줄러 - 타임아웃 처리용 */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // ========== 검색 결과 관련 ==========

    public void setSearchResults(String userId, Object results) {
        searchResults.put(userId, results);
        log.debug("검색 결과 저장 - 사용자: {}", userId);

        // 30초 후 자동 삭제
        scheduler.schedule(() -> {
            if (searchResults.remove(userId) != null) {
                log.debug("검색 결과 타임아웃 - 사용자: {}", userId);
            }
        }, 30, TimeUnit.SECONDS);
    }

    public Object getSearchResults(String userId) {
        return searchResults.get(userId);
    }

    public Object removeSearchResults(String userId) {
        Object results = searchResults.remove(userId);
        log.debug("검색 결과 제거 - 사용자: {}", userId);
        return results;
    }

    public boolean hasSearchResults(String userId) {
        return searchResults.containsKey(userId);
    }

    // ========== 랜덤 재생 선택 관련 ==========

    public void setPendingRandomChoice(String userId, String playlistUrl) {
        pendingRandomChoice.put(userId, playlistUrl);
        log.debug("랜덤 재생 선택 대기 설정 - 사용자: {}", userId);
    }

    public String removePendingRandomChoice(String userId) {
        String url = pendingRandomChoice.remove(userId);
        log.debug("랜덤 재생 선택 대기 제거 - 사용자: {}", userId);
        return url;
    }

    public boolean hasPendingRandomChoice(String userId) {
        return pendingRandomChoice.containsKey(userId);
    }

    // ========== 플레이리스트 이름 입력 관련 ==========

    public void setPendingPlaylistNameInput(String userId, String trackUrl) {
        pendingPlaylistNameInput.put(userId, trackUrl);
        log.debug("플레이리스트 이름 입력 대기 설정 - 사용자: {}", userId);
    }

    public String removePendingPlaylistNameInput(String userId) {
        String url = pendingPlaylistNameInput.remove(userId);
        log.debug("플레이리스트 이름 입력 대기 제거 - 사용자: {}", userId);
        return url;
    }

    public boolean hasPendingPlaylistNameInput(String userId) {
        return pendingPlaylistNameInput.containsKey(userId);
    }

    public void rePutPendingPlaylistNameInput(String userId, String trackUrl) {
        pendingPlaylistNameInput.put(userId, trackUrl);
    }

    // ========== 저장된 플레이리스트 랜덤 재생 선택 관련 ==========

    public void setPendingSavedPlaylistRandomChoice(String userId, String playlistName) {
        pendingSavedPlaylistRandomChoice.put(userId, playlistName);
        log.debug("저장된 플레이리스트 랜덤 재생 선택 대기 설정 - 사용자: {}", userId);
    }

    public String removePendingSavedPlaylistRandomChoice(String userId) {
        String name = pendingSavedPlaylistRandomChoice.remove(userId);
        log.debug("저장된 플레이리스트 랜덤 재생 선택 대기 제거 - 사용자: {}", userId);
        return name;
    }

    public boolean hasPendingSavedPlaylistRandomChoice(String userId) {
        return pendingSavedPlaylistRandomChoice.containsKey(userId);
    }

    public void rePutPendingSavedPlaylistRandomChoice(String userId, String playlistName) {
        pendingSavedPlaylistRandomChoice.put(userId, playlistName);
    }

    public void clearAllPendingStates(String userId) {
        searchResults.remove(userId);
        pendingRandomChoice.remove(userId);
        pendingPlaylistNameInput.remove(userId);
        pendingSavedPlaylistRandomChoice.remove(userId);
        log.debug("모든 대기 상태 제거 - 사용자: {}", userId);
    }
}
