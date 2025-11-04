// 생성됨 - 2025-11-03
package com.kkc.discord_bot.handler;

import com.kkc.discord_bot.music.PlayerManager;
import com.kkc.discord_bot.music.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 음악 재생 명령어 처리 핸들러
 *
 * 음악 재생, 정지, 일시정지, 스킵 등의 기본 재생 제어 기능을 담당합니다.
 *
 * @author KKC
 * @since 2025-11-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MusicCommandHandler {

    private final UserInputStateManager stateManager;

    /**
     * 음악 재생
     * URL 또는 검색어로 음악을 재생합니다.
     *
     * @param event 메시지 이벤트
     * @param text 재생할 음악의 URL 또는 제목
     */
    public void playMusic(MessageReceivedEvent event, String text) {
        if (text.isEmpty()) {
            return;
        }

        if (event.getMember() == null || event.getMember().getVoiceState() == null
                || !event.getMember().getVoiceState().inAudioChannel()) {
            return;
        }

        // 봇이 음성 채널에 없으면 참여
        if (!event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
            final AudioManager audioManager = event.getGuild().getAudioManager();
            final VoiceChannel memberChannel = (VoiceChannel) event.getMember().getVoiceState().getChannel();
            audioManager.openAudioConnection(memberChannel);
        }

        if (text.matches("^(https?://).*")) {
            // URL로 직접 재생
            PlayerManager.getINSTANCE().loadAndPlay(event.getChannel().asTextChannel(), text, event.getMember());
        } else {
            // 검색어로 재생
            searchAndPlay(event, text);
        }
    }

    /**
     * 유튜브 검색 후 재생
     *
     * @param event 메시지 이벤트
     * @param searchText 검색어
     */
    private void searchAndPlay(MessageReceivedEvent event, String searchText) {
        String query = "ytsearch:" + searchText;

        PlayerManager.getINSTANCE().getAudioPlayerManager().loadItem(
                query, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        PlayerManager.getINSTANCE().loadAndPlay(
                                event.getChannel().asTextChannel(),
                                track.getInfo().uri,
                                event.getMember()
                        );
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        List<AudioTrack> tracks = playlist.getTracks();
                        if (tracks.isEmpty()) {
                            return;
                        }

                        List<AudioTrack> topTracks = tracks.subList(0, Math.min(5, tracks.size()));
                        stateManager.setSearchResults(event.getAuthor().getId(), topTracks);

                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setTitle("🔍 검색 결과 (상위 5개)");
                        embed.setColor(Color.ORANGE);

                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < topTracks.size(); i++) {
                            sb.append(i + 1).append(". ").append(topTracks.get(i).getInfo().title).append("\n");
                        }
                        embed.setDescription(sb.toString());
                        embed.setFooter("재생할 곡의 번호를 입력해주세요. (취소: c)");
                    }

                    @Override
                    public void noMatches() {
                        log.warn("검색 결과 없음: {}", searchText);
                    }

                    @Override
                    public void loadFailed(FriendlyException e) {
                        log.error("검색 실패: {}", searchText, e);
                    }
                }
        );
    }

    /**
     * 음악 정지
     * 현재 재생 중인 음악을 정지하고 재생목록을 비웁니다.
     *
     * @param event 메시지 이벤트
     */
    public void stopMusic(MessageReceivedEvent event) {
        if (event.getGuild().getSelfMember().getVoiceState() == null
                || !event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
            return;
        }

        TrackScheduler scheduler = PlayerManager.getINSTANCE()
                                                .getMusicManager(event.getGuild(), event.getChannel().asTextChannel()).scheduler;
        scheduler.clearQueue();
        scheduler.stopTrack();
    }

    /**
     * 다음 곡으로 스킵
     *
     * @param event 메시지 이벤트
     */
    public void skipMusic(MessageReceivedEvent event) {
        TrackScheduler scheduler = PlayerManager.getINSTANCE()
                                                .getMusicManager(event.getGuild(), event.getChannel().asTextChannel()).scheduler;
        scheduler.nextTrack();
    }

    /**
     * 재생목록 섞기
     *
     * @param event 메시지 이벤트
     */
    public void shuffleQueue(MessageReceivedEvent event) {
        TrackScheduler scheduler = PlayerManager.getINSTANCE()
                                                .getMusicManager(event.getGuild(), event.getChannel().asTextChannel()).scheduler;
        scheduler.shuffleQueue();
    }

    /**
     * 음악 일시정지/재개 (토글)
     *
     * @param event 메시지 이벤트
     */
    public void pauseMusic(MessageReceivedEvent event) {
        TrackScheduler scheduler = PlayerManager.getINSTANCE()
                                                .getMusicManager(event.getGuild(), event.getChannel().asTextChannel()).scheduler;
        AudioPlayer player = scheduler.getAudioPlayer();

        if (player.getPlayingTrack() == null) {
            return;
        }

        boolean isPaused = player.isPaused();
        player.setPaused(!isPaused);
    }

    /**
     * 음악 일시정지/재개 (버튼 이벤트)
     *
     * @param event 버튼 이벤트
     */
    public void pauseMusic(ButtonInteractionEvent event) {
        if (event.getGuild() == null) return;

        TrackScheduler scheduler = PlayerManager.getINSTANCE()
                                                .getMusicManager(event.getGuild(), event.getChannel().asTextChannel()).scheduler;
        AudioPlayer player = scheduler.getAudioPlayer();

        if (player.getPlayingTrack() == null) {
            event.reply("❌ 일시정지할 음악이 없습니다.").setEphemeral(true).queue();
            return;
        }

        boolean isPaused = player.isPaused();
        player.setPaused(!isPaused);

        if (player.isPaused()) {
            event.reply("⏸️ **음악을 일시정지했습니다.**").setEphemeral(true).queue();
        } else {
            event.reply("▶️ **음악을 다시 재생합니다.**").setEphemeral(true).queue();
        }
    }

    /**
     * 현재 재생목록 표시
     *
     * @param event 메시지 이벤트
     */
    public void showQueue(MessageReceivedEvent event) {
        TrackScheduler scheduler = PlayerManager.getINSTANCE()
                                                .getMusicManager(event.getGuild(), event.getChannel().asTextChannel()).scheduler;
        AudioPlayer player = scheduler.getAudioPlayer();

        String nowPlaying = (player.getPlayingTrack() != null)
                ? player.getPlayingTrack().getInfo().title
                : "현재 재생 중인 곡이 없습니다.";

        List<String> queueList = new ArrayList<>();
        scheduler.getQueue().forEach(audioTrack -> queueList.add(audioTrack.getInfo().title));

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🎵 현재 재생목록");
        embedBuilder.setColor(Color.ORANGE);
        embedBuilder.addField("▶ 현재 재생 중", nowPlaying, false);

        if (queueList.isEmpty()) {
            embedBuilder.addField("⏳ 대기열", "대기 중인 곡이 없습니다.", false);
        } else {
            StringBuilder sb = new StringBuilder();
            int trackLimit = 10;
            for (int i = 0; i < Math.min(queueList.size(), trackLimit); i++) {
                sb.append(i + 1).append(". ").append(queueList.get(i)).append("\n");
            }
            if (queueList.size() > trackLimit) {
                sb.append("... 외 ").append(queueList.size() - trackLimit).append("곡");
            }
            embedBuilder.addField("📜 대기열", sb.toString(), false);
        }

        event.getChannel().sendMessageEmbeds(embedBuilder.build())
             .setActionRow(
                     Button.primary("music_stop", "🛑 Stop"),
                     Button.primary("music_pause", "⏸️ Pause"),
                     Button.primary("music_shuffle", "🔀 Shuffle"),
                     Button.primary("music_next", "⏭ Next")
             )
             .queue();
    }

    /**
     * 컨트롤 버튼만 표시
     *
     * @param event 메시지 이벤트
     */
    public void showControlButtons(MessageReceivedEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setDescription("아래 버튼을 사용하여 음악을 제어하세요.");
        embedBuilder.setColor(Color.CYAN);

        event.getChannel().sendMessageEmbeds(embedBuilder.build())
             .setActionRow(
                     Button.primary("music_stop", "🛑 Stop"),
                     Button.primary("music_pause", "⏸️ Pause"),
                     Button.primary("music_shuffle", "🔀 Shuffle"),
                     Button.primary("music_next", "⏭ Next")
             )
             .queue();
    }
}