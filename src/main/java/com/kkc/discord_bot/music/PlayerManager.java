// 생성됨 - 2025-10-14
package com.kkc.discord_bot.music;

import com.kkc.discord_bot.constant.BotConstants;
import com.kkc.discord_bot.constant.MessageConstants;
import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * 음악 플레이어 관리자 (Singleton)
 * 
 * 주요 기능:
 * - AudioPlayerManager 인스턴스 관리
 * - Guild별 GuildMusicManager 관리
 * - 음악 로드 및 재생 처리
 * - 플레이리스트 처리
 * 
 * @author KKC
 * @since 2025-10-14
 */
@Slf4j
public class PlayerManager {
    
    // ========== Singleton 인스턴스 ==========
    private static PlayerManager INSTANCE;
    
    // ========== 필드 ==========
    
    /** Guild별 음악 매니저 저장소 (Guild ID -> GuildMusicManager) */
    private final Map<Long, GuildMusicManager> musicManagers;
    
    /** LavaPlayer 오디오 플레이어 매니저 */
    @Getter
    private final AudioPlayerManager audioPlayerManager;
    
    /** 셔플 요청 저장소 (사용자 ID -> 셔플 여부) */
    private final Map<String, Boolean> shuffleRequests = new HashMap<>();

    /**
     * Private 생성자 (Singleton 패턴)
     * AudioPlayerManager를 초기화하고 오디오 소스를 등록합니다.
     */
    private PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();

        // 오디오 품질 설정
        configureAudioQuality();
        
        // 오디오 소스 등록
        registerAudioSources();
        
        log.info("PlayerManager 초기화 완료");
    }

    /**
     * 오디오 품질 설정
     * - Opus 인코딩 품질 최대화
     * - 리샘플링 품질 HIGH로 설정
     * - 필터 핫스왑 활성화
     */
    private void configureAudioQuality() {
        audioPlayerManager.getConfiguration()
                .setOpusEncodingQuality(AudioConfiguration.OPUS_QUALITY_MAX);
        audioPlayerManager.getConfiguration()
                .setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);
        audioPlayerManager.getConfiguration()
                .setFilterHotSwapEnabled(true);
        
        log.debug("오디오 품질 설정 완료");
    }

    /**
     * 오디오 소스 등록
     * - YouTube (YoutubeAudioSourceManager)
     * - 원격 소스 (HTTP, SoundCloud 등)
     * - 로컬 소스
     */
    private void registerAudioSources() {
        // YouTube 소스 등록
        YoutubeAudioSourceManager youtube = new YoutubeAudioSourceManager(true);
        this.audioPlayerManager.registerSourceManager(youtube);

        // 원격 및 로컬 소스 등록
        AudioSourceManagers.registerRemoteSources(this.audioPlayerManager);
        AudioSourceManagers.registerLocalSource(this.audioPlayerManager);
        
        log.debug("오디오 소스 등록 완료");
    }

    /**
     * Singleton 인스턴스 반환
     *
     * @return PlayerManager 인스턴스
     */
    public static PlayerManager getINSTANCE() {
        if (INSTANCE == null) {
            synchronized (PlayerManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PlayerManager();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Guild의 GuildMusicManager 반환 (없으면 생성)
     *
     * @param guild Guild 정보
     * @param textChannel 텍스트 채널 (알림용)
     * @return GuildMusicManager 인스턴스
     */
    public GuildMusicManager getMusicManager(Guild guild, TextChannel textChannel) {
        return this.musicManagers.computeIfAbsent(
                guild.getIdLong(), 
                (guildId) -> {
                    log.info("새로운 GuildMusicManager 생성 - Guild: {}", guild.getName());
                    
                    final GuildMusicManager guildMusicManager = 
                            new GuildMusicManager(this.audioPlayerManager, guild, textChannel);
                    
                    // 오디오 전송 핸들러 설정
                    guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());
                    
                    return guildMusicManager;
                }
        );
    }

    /**
     * Guild의 GuildMusicManager 반환 (VoiceChannelListener에서 사용)
     * TextChannel 없이 조회만 하는 경우 사용
     *
     * @param guild Guild 정보
     * @return GuildMusicManager 인스턴스 (없으면 null)
     */
    public GuildMusicManager getMusicManager(Guild guild) {
        return this.musicManagers.get(guild.getIdLong());
    }

    /**
     * 음악 제목 조회 (비동기 콜백 방식)
     *
     * @param textChannel 텍스트 채널
     * @param trackURL 트랙 URL
     * @param onSuccess 성공 시 콜백 (음악 제목 전달)
     */
    public void findByMusicTitle(
            TextChannel textChannel, 
            String trackURL,
            Consumer<String> onSuccess
    ) {
        final GuildMusicManager musicManager = this.getMusicManager(textChannel.getGuild(), textChannel);

        this.audioPlayerManager.loadItemOrdered(
                musicManager, trackURL,
                new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack audioTrack) {
                        String musicTitle = audioTrack.getInfo().title + " - " + audioTrack.getInfo().author;
                        onSuccess.accept(musicTitle);
                        log.debug("음악 제목 조회 성공: {}", musicTitle);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist audioPlaylist) {
                        // 플레이리스트는 첫 번째 트랙 제목 반환
                        if (!audioPlaylist.getTracks().isEmpty()) {
                            AudioTrack firstTrack = audioPlaylist.getTracks().get(0);
                            String musicTitle = firstTrack.getInfo().title + " - " + firstTrack.getInfo().author;
                            onSuccess.accept(musicTitle);
                        }
                    }

                    @Override
                    public void noMatches() {
                        // textChannel.sendMessage("⚠️ 일치하는 결과가 없습니다. " + trackURL).queue();
                        log.warn("음악 조회 실패 - URL: {}", trackURL);
                    }

                    @Override
                    public void loadFailed(FriendlyException e) {
                        // textChannel.sendMessage("❌ 재생할 수 없습니다. " + e.getMessage()).queue();
                        log.error("음악 로드 실패 - URL: {}", trackURL, e);
                    }
                }
        );
    }

    /**
     * 음악 로드 및 재생
     * 단일 트랙 또는 플레이리스트를 로드하여 재생목록에 추가합니다.
     *
     * @param textChannel 텍스트 채널
     * @param trackURL 트랙 URL
     * @param member 요청한 멤버
     */
    public void loadAndPlay(TextChannel textChannel, String trackURL, Member member) {
        final GuildMusicManager musicManager = this.getMusicManager(textChannel.getGuild(), textChannel);
        
        log.info("음악 로드 시작 - URL: {}, 요청자: {}", trackURL, member.getEffectiveName());
        
        this.audioPlayerManager.loadItemOrdered(
                musicManager, trackURL, 
                new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack audioTrack) {
                        handleTrackLoaded(audioTrack, musicManager, textChannel);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist audioPlaylist) {
                        handlePlaylistLoaded(audioPlaylist, musicManager, textChannel);
                    }

                    @Override
                    public void noMatches() {
                        // textChannel.sendMessage(String.format(MessageConstants.MSG_NO_MATCHES, trackURL)).queue();
                        log.warn("음악 검색 실패 - URL: {}", trackURL);
                    }

                    @Override
                    public void loadFailed(FriendlyException e) {
                        // textChannel.sendMessage(String.format(MessageConstants.MSG_LOAD_FAILED, e.getMessage())).queue();
                        log.error("음악 로드 실패 - URL: {}", trackURL, e);
                    }
                }
        );
    }

    /**
     * 단일 트랙 로드 처리
     *
     * @param audioTrack 로드된 오디오 트랙
     * @param musicManager 음악 매니저
     * @param textChannel 텍스트 채널
     */
    private void handleTrackLoaded(AudioTrack audioTrack, GuildMusicManager musicManager, TextChannel textChannel) {
        musicManager.scheduler.queueAndPlay(audioTrack);
        musicManager.scheduler.checkAndStartAloneTimer();
        
        // textChannel.sendMessageFormat(
        //         MessageConstants.MSG_TRACK_QUEUED,
        //         audioTrack.getInfo().title,
        //         audioTrack.getInfo().author
        // ).queue();
        
        log.info("트랙 추가 완료: {}", audioTrack.getInfo().title);
    }

    /**
     * 플레이리스트 로드 처리
     *
     * @param audioPlaylist 로드된 플레이리스트
     * @param musicManager 음악 매니저
     * @param textChannel 텍스트 채널
     */
    private void handlePlaylistLoaded(AudioPlaylist audioPlaylist, GuildMusicManager musicManager, TextChannel textChannel) {
        AudioTrack firstTrack = audioPlaylist.getSelectedTrack() != null
                ? audioPlaylist.getSelectedTrack()
                : audioPlaylist.getTracks().get(0);

        musicManager.scheduler.queue(firstTrack);
        musicManager.scheduler.checkAndStartAloneTimer();
        
        // textChannel.sendMessageFormat(
        //         MessageConstants.MSG_TRACK_QUEUED,
        //         firstTrack.getInfo().title,
        //         firstTrack.getInfo().author
        // ).queue();
        
        log.info("플레이리스트 첫 트랙 추가: {}", firstTrack.getInfo().title);
    }

    /**
     * 플레이리스트 로드 및 재생
     * 플레이리스트의 모든 트랙을 로드하여 재생목록에 추가합니다.
     *
     * @param channel 텍스트 채널
     * @param playlistUrl 플레이리스트 URL
     * @param member 요청한 멤버
     * @param shuffle 셔플 여부
     */
    public void loadAndPlayPlaylist(
            TextChannel channel,
            String playlistUrl,
            Member member,
            boolean shuffle
    ) {
        GuildMusicManager musicManager = getMusicManager(member.getGuild(), channel);
        
        log.info("플레이리스트 로드 시작 - URL: {}, 셔플: {}", playlistUrl, shuffle);

        audioPlayerManager.loadItemOrdered(
                musicManager, playlistUrl, 
                new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack audioTrack) {
                        handleSingleTrackInPlaylist(audioTrack, musicManager, channel);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        handleFullPlaylist(playlist, musicManager, channel, shuffle);
                    }

                    @Override
                    public void noMatches() {
                        // channel.sendMessage("❌ 찾을 수 없는 트랙입니다.").queue();
                        log.warn("플레이리스트 검색 실패 - URL: {}", playlistUrl);
                    }

                    @Override
                    public void loadFailed(FriendlyException e) {
                        // channel.sendMessage("⚠️ 트랙 로드 중 오류 발생: " + e.getMessage()).queue();
                        log.error("플레이리스트 로드 실패 - URL: {}", playlistUrl, e);
                    }
                }
        );
    }

    /**
     * 플레이리스트 URL이지만 단일 트랙만 로드된 경우 처리
     *
     * @param audioTrack 로드된 트랙
     * @param musicManager 음악 매니저
     * @param channel 텍스트 채널
     */
    private void handleSingleTrackInPlaylist(AudioTrack audioTrack, GuildMusicManager musicManager, TextChannel channel) {
        log.info("단일 트랙 재생 - Queue And Play");
        musicManager.scheduler.queueAndPlay(audioTrack);
        musicManager.scheduler.checkAndStartAloneTimer();

        // channel.sendMessageFormat(
        //         MessageConstants.MSG_TRACK_QUEUED,
        //         audioTrack.getInfo().title,
        //         audioTrack.getInfo().author
        // ).queue();
    }

    /**
     * 전체 플레이리스트 처리
     *
     * @param playlist 플레이리스트
     * @param musicManager 음악 매니저
     * @param channel 텍스트 채널
     * @param shuffle 셔플 여부
     */
    private void handleFullPlaylist(AudioPlaylist playlist, GuildMusicManager musicManager, TextChannel channel, boolean shuffle) {
        if (playlist.getTracks().isEmpty()) {
            // channel.sendMessage("❌ 재생할 트랙이 없습니다.").queue();
            return;
        }

        // 플레이리스트 추가 메시지
        // channel.sendMessageFormat(
        //         MessageConstants.MSG_PLAYLIST_ADDED,
        //         playlist.getName(),
        //         playlist.getTracks().size()
        // ).queue();

        // 트랙 리스트 처리
        List<AudioTrack> tracks = new ArrayList<>(playlist.getTracks());
        
        // 셔플 옵션 처리
        if (shuffle) {
            Collections.shuffle(tracks);
            // channel.sendMessage(MessageConstants.MSG_RANDOM_PLAY_ENABLED).queue();
            log.info("플레이리스트 셔플 완료 - {} 곡", tracks.size());
        }

        // 첫 번째 트랙 즉시 재생
        AudioTrack firstTrack = tracks.remove(0);
        musicManager.scheduler.queueAndPlay(firstTrack);
        musicManager.scheduler.checkAndStartAloneTimer();

        // 나머지 트랙 대기열에 추가
        for (AudioTrack track : tracks) {
            musicManager.scheduler.queue(track);
        }

        log.info("플레이리스트 로드 완료 - {} 곡", playlist.getTracks().size());

        // 재생 목록 표시
        showList(channel, musicManager);
    }

    /**
     * 사용자 응답 처리 (셔플 여부)
     *
     * @param userId 사용자 ID
     * @param response 사용자 응답 (y/yes 또는 n/no)
     * @param musicManager 음악 매니저
     * @param tracks 트랙 리스트
     */
    public void handleUserResponse(
            String userId,
            String response,
            GuildMusicManager musicManager,
            List<AudioTrack> tracks
    ) {
        if (!shuffleRequests.containsKey(userId)) {
            return;
        }

        boolean shuffle = response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes");
        addTracksToQueue(musicManager, tracks, shuffle);
        
        String message = shuffle ? "🔀 **랜덤 재생을 시작합니다!**" : "▶ **순차 재생을 시작합니다!**";
        // musicManager.textChannel.sendMessage(message).queue();
        
        shuffleRequests.remove(userId);
        log.info("사용자 응답 처리 완료 - 셔플: {}", shuffle);
    }

    /**
     * 트랙 리스트를 대기열에 추가
     *
     * @param musicManager 음악 매니저
     * @param tracks 트랙 리스트
     * @param shuffle 셔플 여부
     */
    private void addTracksToQueue(
            GuildMusicManager musicManager,
            List<AudioTrack> tracks,
            boolean shuffle
    ) {
        if (shuffle) {
            Collections.shuffle(tracks);
            log.debug("트랙 리스트 셔플 완료");
        }
        
        for (AudioTrack track : tracks) {
            musicManager.scheduler.queue(track);
        }
        
        log.info("대기열에 {} 곡 추가 완료", tracks.size());
    }

    /**
     * 셔플 요청 확인
     *
     * @param userId 사용자 ID
     * @return 셔플 요청이 있으면 true
     */
    public boolean hasShuffleRequest(String userId) {
        return shuffleRequests.containsKey(userId);
    }

    /**
     * 현재 재생목록 표시
     *
     * @param channel 텍스트 채널
     * @param musicManager 음악 매니저
     */
    public void showList(TextChannel channel, GuildMusicManager musicManager) {
        TrackScheduler scheduler = musicManager.scheduler;
        AudioPlayer player = musicManager.audioPlayer;

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
            int trackLimit = BotConstants.MAX_QUEUE_DISPLAY_LIMIT;
            int trackCount = 0;
            int currentLength = 0;

            for (String track : queueList) {
                if (trackCount >= trackLimit || currentLength + track.length() + 5 > BotConstants.EMBED_FIELD_MAX_CHARACTERS) {
                    break;
                }
                sb.append(trackCount + 1).append(". ").append(track).append("\n");
                currentLength += track.length();
                trackCount++;
            }

            // 더 많은 곡이 있으면 표시
            if (queueList.size() > trackLimit) {
                sb.append("외 ").append(queueList.size() - trackLimit).append("곡...");
            }

            embedBuilder.addField("📜 대기열", sb.toString(), false);
        }

        // 버튼 추가
        Button stopButton = Button.primary("music_stop", "🛑 Stop");
        Button shuffleButton = Button.primary("music_shuffle", "🔀 Shuffle");
        Button nextButton = Button.primary("music_next", "⏭ Next");

        channel.sendMessageEmbeds(embedBuilder.build())
                .setActionRow(stopButton, shuffleButton, nextButton)
                .queue();
        
        log.debug("재생목록 표시 완료");
    }

}
