// 생성됨 - 2025-11-04 자동 GUI 새로고침 기능 추가
// 생성됨 - 2025-10-14 23:32
package com.kkc.discord_bot.music;

import com.kkc.discord_bot.constant.BotConstants;
import com.kkc.discord_bot.constant.MessageConstants;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.*;
import java.util.concurrent.*;

/**
 * 음악 트랙의 재생 순서와 대기열을 관리하는 스케줄러
 * 트랙 재생, 큐 관리, 자동 퇴장 타이머 등을 담당합니다.
 */
@Slf4j
public class TrackScheduler extends AudioEventAdapter {
    // ========== 필드 ==========
    @Getter
    private final AudioPlayer audioPlayer;

    @Getter
    private final BlockingQueue<AudioTrack> queue;

    private final Guild guild;

    /**
     * 메시지 전송을 위한 텍스트 채널
     */
    private final TextChannel textChannel;
    
    /**
     * GuildMusicManager 참조 (GUI 새로고침용)
     */
    private final GuildMusicManager musicManager;

    /**
     * 타이머 작업을 위한 스케줄러
     */
    private ScheduledExecutorService scheduler;

    /**
     * 음악 없을 때 자동 퇴장 타이머
     */
    private ScheduledFuture<?> disconnectTask;

    /**
     * 혼자 있을 때 자동 퇴장 타이머
     */
    private ScheduledFuture<?> aloneDisconnectTask;

    /**
     * 이전에 재생한 트랙들을 저장하는 큐
     */
    private final Queue<AudioTrack> previousTracks;

    /**
     * 반복 재생 여부
     */
    @Getter
    private boolean repeating = false;

    public TrackScheduler(AudioPlayer audioPlayer, Guild guild, TextChannel textChannel, GuildMusicManager musicManager) {
        this.audioPlayer = audioPlayer;
        this.queue = new LinkedBlockingQueue<>();
        this.previousTracks = new LinkedList<>(); // 🔹 이전 트랙 리스트
        this.guild = guild;
        this.textChannel = textChannel;  // 💡 생성자에서 설정
        this.musicManager = musicManager; // 🔹 GuildMusicManager 참조 저장
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void queue(AudioTrack track) {
        this.queue.offer(track);
        // 🔹 새 곡 추가 시 자동 퇴장 타이머 취소
        cancelDisconnectTimer();
        cancelAloneDisconnectTimer();
        // 🔹 혼자 있는지 체크
        checkAndStartAloneTimer();
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        log.info("🎵 onTrackEnd 호출됨 - Guild: {}, EndReason: {}, Track: {}", 
                guild.getName(), endReason, track != null ? track.getInfo().title : "null");
        
        if (endReason.mayStartNext) {
            // 🔹 반복 재생이 켜져있으면 동일한 곡을 다시 재생
            if (repeating && track != null) {
                audioPlayer.startTrack(track.makeClone(), false);
                log.info("🔁 반복 재생 모드 - 같은 곡 다시 재생");
                // 🔹 GUI 자동 새로고침
                refreshGui();
                return;
            }

            // 🔹 이전 트랙 저장
            if (track != null) {
                previousTracks.add(track.makeClone());
            }

            // 🔹 다음 트랙 재생 시도
            nextTrack();
            
            log.info("⏭️ 다음 곡으로 넘어감 - GUI 새로고침 호출");
            // 🔹 다음 곡 재생 시 GUI 자동 새로고침
            refreshGui();
        }

        // 🔹 트랙이 끝나고 대기열도 비어있으면 타이머 시작
        // (nextTrack()에서 이미 처리되지만, 다른 종료 이유에 대비)
        if (!endReason.mayStartNext && queue.isEmpty() && audioPlayer.getPlayingTrack() == null) {
            startDisconnectTimer();
        }
    }

    public void nextTrack() {
        AudioTrack nextTrack = queue.poll();

        if (nextTrack != null) {
            audioPlayer.startTrack(nextTrack, false);
            // 🔹 다음 트랙 재생 시 타이머 취소
            cancelDisconnectTimer();
            cancelAloneDisconnectTimer();
            // 🔹 혼자 있는지 체크
            checkAndStartAloneTimer();
            sendNowPlaying();
        } else {
            // 🔹 대기열이 비어있을 때
            AudioTrack currentTrack = audioPlayer.getPlayingTrack();

            if (currentTrack == null) {
                // 현재 재생 중인 곡도 없으면 타이머 시작
                // textChannel.sendMessage(MessageConstants.MSG_NO_TRACKS).queue();
                startDisconnectTimer();
                log.info("대기열 비어있고 재생 중인 곡 없음 - 타이머 시작");
            } else {
                // 현재 재생 중인 곡이 있으면 메시지만 출력
                // textChannel.sendMessage("⏭️ **다음 곡이 없습니다. 현재 곡이 계속 재생됩니다.**").queue();
                log.info("대기열 비어있지만 현재 곡 재생 중 - Guild: {}", guild.getName());
            }
        }
    }


    /**
     * 음악이 없을 때 30초 후 자동 퇴장 타이머 시작
     * 대기열이 비어있고 현재 재생 중인 곡도 없을 때 호출됩니다.
     */
    private void startDisconnectTimer() {
        cancelDisconnectTimer(); // 기존 타이머가 있다면 취소

        log.info("자동 퇴장 타이머 시작 - Guild: {}", guild.getName());

        disconnectTask = scheduler.schedule(
                () -> {
                    AudioManager audioManager = guild.getAudioManager();
                    if (audioManager.isConnected()) {
                        log.info("음악 없음 - 음성 채널 퇴장 - Guild: {}", guild.getName());
                        audioManager.closeAudioConnection();

                        if (textChannel != null) {
                            // textChannel.sendMessage(MessageConstants.MSG_DISCONNECT_NO_MUSIC).queue();
                        }

                        // 🔹 리소스 정리
                        clearQueue();
                        stopTrack();
                        previousTracks.clear();
                    }
                },
                BotConstants.DISCONNECT_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /**
     * 자동 퇴장 타이머 취소
     * 새로운 곡이 추가되거나 재생이 시작될 때 호출됩니다.
     */
    private void cancelDisconnectTimer() {
        if (disconnectTask != null && !disconnectTask.isDone()) {
            disconnectTask.cancel(true);
            log.debug("자동 퇴장 타이머 취소 - Guild: {}", guild.getName());
        }
    }

    public boolean isQueueEmpty() {
        return queue.isEmpty() && audioPlayer.getPlayingTrack() == null;
    }

    public boolean isLastTrack() {
        return queue.isEmpty() && audioPlayer.getPlayingTrack() != null;
    }

    public void clearQueue() {
        queue.clear();
    }

    public void stopTrack() {
        audioPlayer.stopTrack();
    }


    public void previousTrack() {
        if (previousTracks.isEmpty()) {
            if (textChannel != null) {
                // textChannel.sendMessage("⏪ **이전 곡이 없습니다!**").queue();
            }
            return;
        }

        // 현재 곡이 있다면 이전 트랙 스택에 추가 (루프 방지)
        if (audioPlayer.getPlayingTrack() != null) {
            previousTracks.add(audioPlayer.getPlayingTrack().makeClone());
        }

        // 🔹 이전 곡을 가져와 재생
        AudioTrack previous = previousTracks.poll();
        if (previous != null) {
            audioPlayer.startTrack(previous.makeClone(), false);
            sendNowPlaying();
        }
    }

    public void sendNowPlaying() {
        if (textChannel != null && audioPlayer.getPlayingTrack() != null) {
            AudioTrack currentTrack = audioPlayer.getPlayingTrack();
            //textChannel.sendMessage("🎵 **현재 재생 중:** " + currentTrack.getInfo().title).queue();
        }
    }

    public void playTrack(AudioTrack track) {
        if (track != null) {
            audioPlayer.startTrack(track, false); // 🔹 강제 실행
        }
    }

    public void queueAndPlay(AudioTrack track) {
        // 🔹 새 곡 추가 시 자동 퇴장 타이머 취소
        cancelDisconnectTimer();
        cancelAloneDisconnectTimer();

        if (this.audioPlayer.getPlayingTrack() == null) {
            this.audioPlayer.startTrack(track, false);
            sendNowPlaying(); // 현재 재생 중 메시지 출력
        } else {
            this.queue.add(track);
        }

        // 🔹 혼자 있는지 체크
        checkAndStartAloneTimer();
    }

    // TrackScheduler.java 내에 추가할 메서드
    public void shuffleQueue() {
        if (queue.isEmpty()) {
            // textChannel.sendMessage("⚠️ 대기열에 곡이 없어서 셔플할 수 없습니다.").queue();
            return;
        }

        List<AudioTrack> tracks = new ArrayList<>(queue);
        Collections.shuffle(tracks);


        queue.clear(); // 기존 큐 비우기
        queue.addAll(tracks);

        // textChannel.sendMessage("🔀 **재생 대기열을 랜덤으로 섞었습니다!**").queue();
    }

    // 🔹 음성 채널에 봇만 남아있는지 체크하고 타이머 시작
    public void checkAndStartAloneTimer() {
        AudioManager audioManager = guild.getAudioManager();
        if (!audioManager.isConnected()) {
            return;
        }

        VoiceChannel voiceChannel = (VoiceChannel) audioManager.getConnectedChannel();
        if (voiceChannel == null) {
            return;
        }

        // 봇을 제외한 멤버 수 확인
        long humanCount = voiceChannel.getMembers().stream()
                                      .filter(member -> !member.getUser().isBot())
                                      .count();

        if (humanCount == 0) {
            startAloneDisconnectTimer();
        } else {
            cancelAloneDisconnectTimer();
        }
    }

    // 🔹 혼자 있을 때 30초 후 자동 퇴장 타이머 시작
    private void startAloneDisconnectTimer() {
        cancelAloneDisconnectTimer(); // 기존 타이머가 있다면 취소

        aloneDisconnectTask = scheduler.schedule(
                () -> {
                    AudioManager audioManager = guild.getAudioManager();
                    if (audioManager.isConnected()) {
                        VoiceChannel voiceChannel = (VoiceChannel) audioManager.getConnectedChannel();
                        if (voiceChannel != null) {
                            long humanCount = voiceChannel.getMembers().stream()
                                                          .filter(member -> !member.getUser().isBot())
                                                          .count();

                            if (humanCount == 0) {
                                audioManager.closeAudioConnection();
                                if (textChannel != null) {
                                    // textChannel.sendMessage("👋 30초 동안 음성 채널에 사람이 없어서 나갑니다.").queue();
                                }
                                clearQueue(); // 🔹 큐도 정리
                                stopTrack(); // 🔹 재생 중인 곡도 정지
                                previousTracks.clear(); // 🔹 이전 트랙도 정리
                            }
                        }
                    }
                }, 30, TimeUnit.SECONDS
        );
    }

    // 🔹 혼자 있을 때 타이머 취소
    private void cancelAloneDisconnectTimer() {
        if (aloneDisconnectTask != null && !aloneDisconnectTask.isDone()) {
            aloneDisconnectTask.cancel(true);
        }
    }

    /**
     * 반복 재생 설정
     *
     * @param repeating true면 현재 곡을 반복 재생, false면 다음 곡으로 넘어감
     */
    public void setRepeating(boolean repeating) {
        this.repeating = repeating;
        log.info("반복 재생 설정 변경 - {} - Guild: {}", repeating, guild.getName());
    }
    
    /**
     * GUI 메시지 자동 새로고침
     * 곡이 변경될 때 자동으로 GUI를 업데이트합니다.
     */
    private void refreshGui() {
        try {
            String messageId = musicManager.getGuiMessageId();
            log.debug("🔄 refreshGui 호출됨 - Guild: {}, MessageId: {}, TextChannel: {}", 
                     guild.getName(), messageId, textChannel != null ? textChannel.getName() : "null");
            
            if (messageId == null) {
                log.warn("GUI 메시지 ID가 null입니다 - Guild: {}", guild.getName());
                return;
            }
            
            if (textChannel == null) {
                log.warn("TextChannel이 null입니다 - Guild: {}", guild.getName());
                return;
            }
            
            // 메시지를 가져와서 수정
            textChannel.retrieveMessageById(messageId).queue(
                    message -> {
                        log.debug("GUI 메시지 찾음 - Guild: {}, MessageId: {}", guild.getName(), messageId);
                        // Embed 생성
                        net.dv8tion.jda.api.EmbedBuilder embed = createMusicGuiEmbed();
                        // 버튼 생성
                        List<net.dv8tion.jda.api.interactions.components.ActionRow> buttons = createMusicGuiButtons();
                        
                        // 메시지 수정
                        message.editMessageEmbeds(embed.build())
                               .setComponents(buttons)
                               .queue(
                                       success -> log.info("✅ GUI 자동 새로고침 성공 - Guild: {}", guild.getName()),
                                       error -> log.error("❌ GUI 자동 새로고침 실패 - Guild: {}, Error: {}", guild.getName(), error.getMessage())
                               );
                    },
                    error -> log.error("❌ GUI 메시지를 찾을 수 없음 - Guild: {}, MessageId: {}, Error: {}", guild.getName(), messageId, error.getMessage())
            );
        } catch (Exception e) {
            log.error("❌ GUI 새로고침 중 오류 발생 - Guild: {}", guild.getName(), e);
        }
    }
    
    /**
     * 음악 GUI Embed 생성
     */
    private net.dv8tion.jda.api.EmbedBuilder createMusicGuiEmbed() {
        net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
        embed.setTitle("🎵 음악 제어 패널");
        embed.setColor(java.awt.Color.CYAN);

        // 현재 재생 중인 곡
        AudioTrack currentTrack = audioPlayer.getPlayingTrack();
        if (currentTrack != null) {
            String trackInfo = String.format(
                    "**%s**\n진행: %s / %s",
                    currentTrack.getInfo().title,
                    formatTime(currentTrack.getPosition()),
                    formatTime(currentTrack.getDuration())
            );
            embed.addField("▶ 현재 재생 중", trackInfo, false);
        } else {
            embed.addField("▶ 현재 재생 중", "재생 중인 곡이 없습니다", false);
        }

        // 재생 상태
        String status = audioPlayer.isPaused() ? "⏸ 일시정지" : "▶ 재생 중";
        embed.addField("상태", status, true);

        // 대기열 크기
        int queueSize = queue.size();
        embed.addField("📜 대기열", queueSize + "곡", true);

        embed.setFooter("음악 봇 GUI", null);
        embed.setTimestamp(java.time.Instant.now());

        return embed;
    }
    
    /**
     * 음악 GUI 버튼 생성
     */
    private List<net.dv8tion.jda.api.interactions.components.ActionRow> createMusicGuiButtons() {
        List<net.dv8tion.jda.api.interactions.components.ActionRow> rows = new ArrayList<>();

        // 첫 번째 줄: 재생 컨트롤
        net.dv8tion.jda.api.interactions.components.buttons.Button playBtn = 
                net.dv8tion.jda.api.interactions.components.buttons.Button.success("music_gui_play", "▶ 재생");
        net.dv8tion.jda.api.interactions.components.buttons.Button pauseBtn = 
                net.dv8tion.jda.api.interactions.components.buttons.Button.secondary("music_gui_pause", "⏸ 일시정지");
        net.dv8tion.jda.api.interactions.components.buttons.Button stopBtn = 
                net.dv8tion.jda.api.interactions.components.buttons.Button.danger("music_gui_stop", "⏹ 정지");
        net.dv8tion.jda.api.interactions.components.buttons.Button skipBtn = 
                net.dv8tion.jda.api.interactions.components.buttons.Button.primary("music_gui_skip", "⏭ 스킵");
        net.dv8tion.jda.api.interactions.components.buttons.Button addBtn = 
                net.dv8tion.jda.api.interactions.components.buttons.Button.success("music_gui_add", "➕ 음악 추가");

        rows.add(net.dv8tion.jda.api.interactions.components.ActionRow.of(playBtn, pauseBtn, stopBtn, skipBtn, addBtn));

        // 두 번째 줄: 재생목록 및 설정
        net.dv8tion.jda.api.interactions.components.buttons.Button queueBtn = 
                net.dv8tion.jda.api.interactions.components.buttons.Button.secondary("music_gui_queue", "📜 현재 대기열");
        net.dv8tion.jda.api.interactions.components.buttons.Button dbPlaylistBtn = 
                net.dv8tion.jda.api.interactions.components.buttons.Button.secondary("music_gui_db_playlist", "💾 저장된 재생목록");
        net.dv8tion.jda.api.interactions.components.buttons.Button shuffleBtn = 
                net.dv8tion.jda.api.interactions.components.buttons.Button.primary("music_gui_shuffle", "🔀 셔플");

        rows.add(net.dv8tion.jda.api.interactions.components.ActionRow.of(queueBtn, dbPlaylistBtn, shuffleBtn));

        // 세 번째 줄: 재생목록 관리
        net.dv8tion.jda.api.interactions.components.buttons.Button addPlaylistBtn = 
                net.dv8tion.jda.api.interactions.components.buttons.Button.success("music_gui_add_playlist", "➕ 재생목록 추가");
        net.dv8tion.jda.api.interactions.components.buttons.Button editPlaylistBtn = 
                net.dv8tion.jda.api.interactions.components.buttons.Button.primary("music_gui_edit_playlist", "✏️ 재생목록 편집");
        net.dv8tion.jda.api.interactions.components.buttons.Button clearBtn = 
                net.dv8tion.jda.api.interactions.components.buttons.Button.danger("music_gui_clear", "🗑 대기열 비우기");

        rows.add(net.dv8tion.jda.api.interactions.components.ActionRow.of(addPlaylistBtn, editPlaylistBtn, clearBtn));

        // 네 번째 줄: 새로고침
        net.dv8tion.jda.api.interactions.components.buttons.Button refreshBtn = 
                net.dv8tion.jda.api.interactions.components.buttons.Button.secondary("music_gui_refresh", "🔄 새로고침");

        rows.add(net.dv8tion.jda.api.interactions.components.ActionRow.of(refreshBtn));

        return rows;
    }
    
    /**
     * 시간 포맷팅 (밀리초 -> MM:SS)
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

}