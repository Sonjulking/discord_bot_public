// 생성됨 - 2025-11-03
package com.kkc.discord_bot.handler;

import com.kkc.discord_bot.music.PlayerManager;
import com.kkc.discord_bot.music.TrackScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.springframework.stereotype.Component;

/**
 * 버튼 인터랙션 처리 핸들러
 * 
 * 음악 컨트롤 버튼 클릭 이벤트를 처리합니다.
 * 
 * @author KKC
 * @since 2025-11-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ButtonInteractionHandler {
    
    private final MusicCommandHandler musicCommandHandler;
    
    /**
     * 버튼 인터랙션 처리
     * 
     * @param event 버튼 이벤트
     */
    public void handleButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        if (event.getGuild() == null) return;

        TrackScheduler scheduler = PlayerManager.getINSTANCE()
                .getMusicManager(event.getGuild(), event.getChannel().asTextChannel()).scheduler;

        switch (buttonId) {
            case "music_next":
                scheduler.nextTrack();
                event.reply("⏭️ 다음 곡을 재생합니다.").setEphemeral(true).queue();
                log.info("버튼: 다음 곡");
                break;
                
            case "music_stop":
                scheduler.clearQueue();
                scheduler.stopTrack();
                event.reply("🛑 음악 재생을 중지했습니다.").setEphemeral(true).queue();
                log.info("버튼: 정지");
                break;
                
            case "music_shuffle":
                scheduler.shuffleQueue();
                event.reply("🔀 재생목록을 섞었습니다.").setEphemeral(true).queue();
                log.info("버튼: 셔플");
                break;
                
            case "music_pause":
                musicCommandHandler.pauseMusic(event);
                log.info("버튼: 일시정지/재개");
                break;
                
            default:
                log.warn("알 수 없는 버튼 ID: {}", buttonId);
                break;
        }
    }
}
