// 생성됨 - 2025-11-03
package com.kkc.discord_bot.handler;

import com.kkc.discord_bot.music.PlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 검색 결과 처리 핸들러
 * 
 * 음악 검색 결과 선택을 처리합니다.
 * 
 * @author KKC
 * @since 2025-11-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchCommandHandler {
    
    private final UserInputStateManager stateManager;
    
    /**
     * 검색 결과 선택 처리
     * 
     * @param event 메시지 이벤트
     * @param msg 사용자 입력 (1-5 또는 c)
     */
    @SuppressWarnings("unchecked")
    public void handleSearchResultInteraction(MessageReceivedEvent event, String msg) {
        String authorId = event.getAuthor().getId();
        
        if (msg.equalsIgnoreCase("c")) {
            stateManager.removeSearchResults(authorId);
            return;
        }

        if (msg.matches("[1-5]")) {
            int selectedIndex = Integer.parseInt(msg) - 1;
            List<AudioTrack> tracks = (List<AudioTrack>) stateManager.removeSearchResults(authorId);
            
            if (tracks != null && selectedIndex < tracks.size()) {
                AudioTrack selectedTrack = tracks.get(selectedIndex);
                PlayerManager.getINSTANCE().loadAndPlay(
                        event.getChannel().asTextChannel(), 
                        selectedTrack.getInfo().uri, 
                        event.getMember()
                );
                log.info("검색 결과 선택: {} - {}", selectedIndex + 1, selectedTrack.getInfo().title);
            }
        }
    }
}
