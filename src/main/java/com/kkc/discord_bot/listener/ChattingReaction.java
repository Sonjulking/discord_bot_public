// 생성됨 - 2025-11-03
package com.kkc.discord_bot.listener;

import com.kkc.discord_bot.handler.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.awt.*;

/**
 * 채팅 반응 리스너
 *
 * 디스코드 메시지와 버튼 인터랙션을 받아 적절한 핸들러로 라우팅합니다.
 *
 * @author KKC
 * @since 2025-11-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChattingReaction extends ListenerAdapter {

    private final UserInputStateManager stateManager;
    private final MusicCommandHandler musicCommandHandler;
    private final PlaylistCommandHandler playlistCommandHandler;
    private final SearchCommandHandler searchCommandHandler;
    private final ButtonInteractionHandler buttonInteractionHandler;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String authorId = event.getAuthor().getId();
        String msg = event.getMessage().getContentRaw();

        // 사용자 입력 대기 상태 확인 및 처리
        if (stateManager.hasPendingPlaylistNameInput(authorId)) {
            playlistCommandHandler.handlePlaylistNameInput(event, msg);
            return;
        }

        if (stateManager.hasSearchResults(authorId)) {
            searchCommandHandler.handleSearchResultInteraction(event, msg);
            return;
        }

        if (stateManager.hasPendingRandomChoice(authorId)) {
            handleRandomChoice(event, msg);
            return;
        }

        if (stateManager.hasPendingSavedPlaylistRandomChoice(authorId)) {
            playlistCommandHandler.handleSavedPlaylistRandomChoice(event, msg);
            return;
        }

        // 명령어가 아니면 무시
        if (!msg.startsWith("!")) return;

        // 명령어 처리
        handleCommand(event);
    }

    /**
     * 명령어 라우팅
     *
     * @param event 메시지 이벤트
     */
    private void handleCommand(MessageReceivedEvent event) {
        String[] parts = event.getMessage().getContentRaw().split(" ", 2);
        String command = parts[0].toLowerCase();
        String argument = (parts.length > 1) ? parts[1] : "";

        switch (command) {
            case "!ping":
            case "!핑":
                // event.getChannel().sendMessage("Pong!").queue();
                break;

            case "!play":
            case "!p":
            case "!노래":
                musicCommandHandler.playMusic(event, argument);
                break;

            case "!next":
            case "!skip":
            case "!s":
            case "!n":
                musicCommandHandler.skipMusic(event);
                break;

            case "!stop":
                musicCommandHandler.stopMusic(event);
                break;

            case "!help":
            case "!도움말":
                sendHelpMessage(event);
                break;

            case "!list":
            case "!q":
            case "!재생목록":
                musicCommandHandler.showQueue(event);
                break;

            case "!shuffle":
                musicCommandHandler.shuffleQueue(event);
                break;

            case "!save":
            case "!add":
                playlistCommandHandler.saveMusicList(event, argument);
                break;

            case "!pl":
                playlistCommandHandler.playMusicList(event, argument);
                break;

            case "!showpl":
            case "!목록보기":
                playlistCommandHandler.showSavedMusicList(event);
                break;

            case "!pause":
            case "!ps":
                musicCommandHandler.pauseMusic(event);
                break;

            case "!button":
                musicCommandHandler.showControlButtons(event);
                break;

            default:
                log.debug("알 수 없는 명령어: {}", command);
                break;
        }
    }

    /**
     * 랜덤 재생 선택 처리 (URL 플레이리스트용)
     * 이 기능은 현재 구현되어 있지 않아 보입니다.
     *
     * @param event 메시지 이벤트
     * @param msg 사용자 입력
     */
    private void handleRandomChoice(MessageReceivedEvent event, String msg) {
        String authorId = event.getAuthor().getId();
        String playlistUrl = stateManager.removePendingRandomChoice(authorId);
        String choice = msg.toLowerCase();

        // TODO: 이 기능은 PlayerManager에 구현이 필요합니다.
        if (choice.equals("y") || choice.equals("yes")) {
            log.info("랜덤 재생 선택: {}", playlistUrl);
            // enableRandomPlayback(event, playlistUrl);
        } else if (choice.equals("n") || choice.equals("no")) {
            log.info("순차 재생 선택: {}", playlistUrl);
            // normalPlaylistPlayback(event, playlistUrl);
        } else {
            stateManager.setPendingRandomChoice(authorId, playlistUrl);
        }
    }

    /**
     * 도움말 메시지 표시
     *
     * @param event 메시지 이벤트
     */
    private void sendHelpMessage(MessageReceivedEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🎵 디스코드 봇 명령어 목록");
        embed.setColor(Color.CYAN);
        embed.addField("!play, !p <제목 또는 URL>", "음악을 재생합니다.", false);
        embed.addField("!skip, !n", "다음 곡으로 건너뜁니다.", false);
        embed.addField("!stop", "음악 재생을 중지하고 대기열을 비웁니다.", false);
        embed.addField("!list, !q", "현재 재생 목록을 보여줍니다.", false);
        embed.addField("!shuffle", "재생 목록을 무작위로 섞습니다.", false);
        embed.addField("!save <URL>", "URL의 음악/플레이리스트를 내 목록에 저장합니다.", false);
        embed.addField("!pl <이름>", "저장된 플레이리스트를 재생합니다. (이름이 없으면 랜덤 재생)", false);
        embed.addField("!showpl", "저장된 모든 플레이리스트를 보여줍니다.", false);
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        buttonInteractionHandler.handleButtonInteraction(event);
    }
}