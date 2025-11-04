// 생성됨 - 2025-11-04 자동 GUI 새로고침 기능 추가
// 생성됨 - 2025-10-30 02:53:52
package com.kkc.discord_bot.listener;

import com.kkc.discord_bot.music.GuildMusicManager;
import com.kkc.discord_bot.music.PlayerManager;
import com.kkc.discord_bot.music.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ComponentInteraction;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.managers.AudioManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

/**
 * 음악 봇 GUI 리스너
 * !gui 명령어로 음악 기능을 디스코드 Embed와 버튼으로 조작합니다.
 * <p>
 * 주요 기능:
 * - 음악 재생 GUI 표시
 * - 재생/일시정지/정지/스킵 버튼
 * - 볼륨 조절
 * - 재생목록 조회 및 관리
 * - 셔플 및 반복 재생
 *
 * @author KKC
 * @since 2025-10-28
 */
@Slf4j
@RequiredArgsConstructor
public class MusicGuiListener extends ListenerAdapter {

    // ========== 의존성 주입 ==========//
    private final com.kkc.discord_bot.service.MusicListService musicListService;


    // ========== GUI 명령어 ==========//
    private static final String GUI_COMMAND = "!gui";

    // ========== 버튼 ID ==========//
    private static final String BTN_PLAY = "music_gui_play";
    private static final String BTN_PAUSE = "music_gui_pause";
    private static final String BTN_STOP = "music_gui_stop";
    private static final String BTN_SKIP = "music_gui_skip";
    private static final String BTN_QUEUE = "music_gui_queue";
    private static final String BTN_VOLUME = "music_gui_volume";
    private static final String BTN_SHUFFLE = "music_gui_shuffle";
    private static final String BTN_REPEAT = "music_gui_repeat";
    private static final String BTN_ADD_MUSIC = "music_gui_add";
    private static final String BTN_CLEAR_QUEUE = "music_gui_clear";
    private static final String BTN_REFRESH = "music_gui_refresh";
    private static final String BTN_DB_PLAYLIST = "music_gui_db_playlist";
    private static final String BTN_ADD_PLAYLIST = "music_gui_add_playlist";
    private static final String BTN_EDIT_PLAYLIST = "music_gui_edit_playlist";
    private static final String BTN_PLAYLIST_EDIT_PREV = "pl_edit_prev";
    private static final String BTN_PLAYLIST_EDIT_NEXT = "pl_edit_next";
    private static final String BTN_PLAYLIST_DELETE_SONG = "pl_edit_delete_song";
    private static final String BTN_CLOSE_PLAYLIST_EDIT = "close_playlist_edit";
    private static final String BTN_ADD_SONG_TO_PLAYLIST = "add_song_to_playlist";
    private static final String BTN_DELETE_ALL_PLAYLIST = "delete_all_playlist";


    // ========== 모달 ID ==========//
    private static final String MODAL_ADD_MUSIC = "modal_add_music";
    private static final String MODAL_VOLUME = "modal_volume";
    private static final String MODAL_ADD_PLAYLIST = "modal_add_playlist";
    private static final String INPUT_MUSIC_URL = "input_music_url";
    private static final String INPUT_VOLUME = "input_volume";
    private static final String INPUT_PLAYLIST_URL = "input_playlist_url";
    private static final String INPUT_PLAYLIST_NAME = "input_playlist_name";

    // ========== 셀렉트 메뉴 ID ==========//
    private static final String SELECT_SEARCH_RESULT = "select_search_result";
    private static final String SELECT_DB_PLAYLIST = "select_db_playlist";
    private static final String SELECT_EDIT_PLAYLIST = "select_edit_playlist";
    private static final String SELECT_PLAYLIST_SONGS = "select_playlist_songs";
    private static final String SELECT_PLAYLIST_SHUFFLE = "select_playlist_shuffle";
    private static final String SELECT_EDIT_ACTION = "select_edit_action";
    private static final String SELECT_ADD_SONG_TO_PLAYLIST = "select_add_song_to_playlist";
    private static final String SELECT_DB_PLAYLIST_SHUFFLE_CHOICE = "select_db_playlist_shuffle_choice";
    private static final String SELECT_QUEUE_PLAYLIST_SHUFFLE = "select_queue_playlist_shuffle";


    // ========== 모달 ID (추가) ==========//
    private static final String MODAL_ADD_SONG_TO_PLAYLIST = "modal_add_song_to_playlist";
    private static final String INPUT_SONG_URL = "input_song_url";

    /**
     * 메시지 수신 이벤트 처리
     * !gui 명령어를 감지하여 음악 GUI를 표시합니다.
     *
     * @param event 메시지 수신 이벤트
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // 봇이 보낸 메시지인지 확인 (무한 루프 방지)
        if (event.getAuthor().isBot()) {
            return;
        }

        String message = event.getMessage().getContentDisplay().trim();

        if (message.equalsIgnoreCase(GUI_COMMAND)) {
            log.info("GUI 명령어 실행 - 사용자: {}", event.getAuthor().getName());
            showMusicGui(event.getChannel().asTextChannel(), event.getMember());
        }
    }

    /**
     * 음악 GUI 표시
     * Embed와 버튼으로 구성된 음악 제어 패널을 생성합니다.
     *
     * @param channel 텍스트 채널
     * @param member  요청한 멤버
     */
    private void showMusicGui(TextChannel channel, Member member) {

        // 현재 길드(서버) 정보를 가져옴
        Guild guild = channel.getGuild();
        // PlayerManager를 통해 이 길드 전용 MusicManager를 가져옴
        GuildMusicManager musicManager = PlayerManager.getINSTANCE().getMusicManager(guild, channel);
        // MusicManager에서 실제 오디오 플레이어를 가져옴
        AudioPlayer player = musicManager.audioPlayer;

        // Embed 생성
        EmbedBuilder embed = createMusicGuiEmbed(player, musicManager);

        // 버튼 생성
        List<ActionRow> actionRows = createMusicGuiButtons(player);

        // 메시지 전송 및 메시지 ID 저장
        channel.sendMessageEmbeds(embed.build())
               .setComponents(actionRows)
               .queue(message -> {
                   // 🔹 GUI 메시지 ID 저장 (자동 새로고침용)
                   musicManager.setGuiMessageId(message.getId());
                   log.info("✅ GUI 메시지 ID 저장 완료 - Guild: {}, Channel: {}, MessageId: {}", 
                           guild.getName(), channel.getName(), message.getId());
               }, error -> {
                   log.error("❌ GUI 메시지 전송 실패 - Guild: {}, Error: {}", guild.getName(), error.getMessage());
               });

        log.info("GUI 표시 요청 완료 - Guild: {}", guild.getName());
    }

    /**
     * 음악 GUI Embed 생성
     *
     * @param player       오디오 플레이어
     * @param musicManager 음악 매니저
     * @return EmbedBuilder
     */
    private EmbedBuilder createMusicGuiEmbed(AudioPlayer player, GuildMusicManager musicManager) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🎵 음악 제어 패널");
        embed.setColor(Color.CYAN);

        // 현재 재생 중인 곡
        AudioTrack currentTrack = player.getPlayingTrack();
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
        String status = player.isPaused() ? "⏸ 일시정지" : "▶ 재생 중";
        embed.addField("상태", status, true);

        // 볼륨
        //embed.addField("🔊 볼륨", player.getVolume() + "%", true);

        // 대기열 크기
        int queueSize = musicManager.scheduler.getQueue().size();
        embed.addField("📜 대기열", queueSize + "곡", true);

        // 반복 재생 상태
        //String repeatStatus = musicManager.scheduler.isRepeating() ? "🔁 켜짐" : "➡ 꺼짐";
        //embed.addField("반복 재생", repeatStatus, true);

        embed.setFooter("음악 봇 GUI", null);
        embed.setTimestamp(java.time.Instant.now());

        return embed;
    }

    /**
     * 음악 GUI 버튼 생성
     *
     * @param player 오디오 플레이어
     * @return ActionRow 리스트
     */
    private List<ActionRow> createMusicGuiButtons(AudioPlayer player) {
        List<ActionRow> rows = new ArrayList<>();

        // 첫 번째 줄: 재생 컨트롤
        Button playBtn = Button.success(BTN_PLAY, "▶ 재생");
        Button pauseBtn = Button.secondary(BTN_PAUSE, "⏸ 일시정지");
        Button stopBtn = Button.danger(BTN_STOP, "⏹ 정지");
        Button skipBtn = Button.primary(BTN_SKIP, "⏭ 스킵");
        Button addBtn = Button.success(BTN_ADD_MUSIC, "➕ 음악 추가");

        rows.add(ActionRow.of(playBtn, pauseBtn, stopBtn, skipBtn, addBtn));

        // 두 번째 줄: 재생목록 및 설정
        Button queueBtn = Button.secondary(BTN_QUEUE, "📜 현재 대기열");
        Button dbPlaylistBtn = Button.secondary(BTN_DB_PLAYLIST, "💾 저장된 재생목록");
        //Button volumeBtn = Button.secondary(BTN_VOLUME, "🔊 볼륨");
        Button shuffleBtn = Button.primary(BTN_SHUFFLE, "🔀 셔플");

        rows.add(ActionRow.of(queueBtn, dbPlaylistBtn, /*volumeBtn,*/shuffleBtn));

        // 세 번째 줄: 재생목록 관리
        Button addPlaylistBtn = Button.success(BTN_ADD_PLAYLIST, "➕ 재생목록 추가");
        Button editPlaylistBtn = Button.primary(BTN_EDIT_PLAYLIST, "✏️ 재생목록 편집");
        //Button repeatBtn = Button.primary(BTN_REPEAT, "🔁 반복");
        Button clearBtn = Button.danger(BTN_CLEAR_QUEUE, "🗑 대기열 비우기");

        rows.add(ActionRow.of(addPlaylistBtn, editPlaylistBtn, /*repeatBtn,*/ clearBtn));

        // 네 번째 줄: 새로고침
        Button refreshBtn = Button.secondary(BTN_REFRESH, "🔄 새로고침");

        rows.add(ActionRow.of(refreshBtn));

        return rows;
    }

    /**
     * 시간 포맷팅 (밀리초 -> MM:SS)
     *
     * @param milliseconds 밀리초
     * @return 포맷된 시간 문자열
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 버튼 클릭 이벤트 처리
     *
     * @param event 버튼 인터랙션 이벤트
     */
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        // 음악 GUI 버튼이 아니면 무시
        if (!buttonId.startsWith("music_gui_") && !buttonId.startsWith("delete_playlist_")
                && !buttonId.startsWith("confirm_delete_playlist:") && !buttonId.startsWith("cancel_delete_playlist")
                && !buttonId.startsWith("pl_edit_") && !buttonId.equals(BTN_CLOSE_PLAYLIST_EDIT)
                && !buttonId.startsWith(BTN_ADD_SONG_TO_PLAYLIST) && !buttonId.startsWith(BTN_DELETE_ALL_PLAYLIST)) {
            return;
        }

        Member member = event.getMember();
        if (member == null) {
            event.reply("❌ 멤버 정보를 가져올 수 없습니다.").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ 서버 정보를 가져올 수 없습니다.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        GuildMusicManager musicManager = PlayerManager.getINSTANCE().getMusicManager(guild, channel);

        log.info("음악 GUI 버튼 클릭 - ID: {}, 사용자: {}", buttonId, event.getUser().getName());

        if (buttonId.startsWith(BTN_PLAYLIST_DELETE_SONG)) {
            handleDeleteSongFromPlaylist(event, buttonId);
            return;
        }
        if (buttonId.startsWith(BTN_PLAYLIST_EDIT_PREV)) {
            handlePlaylistEditPrev(event, buttonId);
            return;
        }
        if (buttonId.startsWith(BTN_PLAYLIST_EDIT_NEXT)) {
            handlePlaylistEditNext(event, buttonId);
            return;
        }
        if (buttonId.equals(BTN_CLOSE_PLAYLIST_EDIT)) {
            event.editMessage("재생목록 편집을 종료했습니다.").setComponents().queue();
            return;
        }
        if (buttonId.startsWith(BTN_ADD_SONG_TO_PLAYLIST)) {
            String playlistName = buttonId.substring(BTN_ADD_SONG_TO_PLAYLIST.length() + 1);
            handleAddSongToPlaylistModal(event, playlistName);
            return;
        }
        if (buttonId.startsWith(BTN_DELETE_ALL_PLAYLIST)) {
            String playlistName = buttonId.substring(BTN_DELETE_ALL_PLAYLIST.length() + 1);
            handleConfirmDeletePlaylist(event, playlistName);
            return;
        }

        // 재생목록 삭제 확인 버튼 처리
        if (buttonId.startsWith("confirm_delete_playlist:")) {
            String playlistName = buttonId.substring("confirm_delete_playlist:".length());
            handleDeletePlaylist(event, playlistName);
            return;
        }

        // 취소 버튼 처리
        if (buttonId.equals("cancel_delete_playlist")) {
            event.editMessage("❌ 삭제가 취소되었습니다.").setComponents().queue();
            return;
        }

        // 재생목록 전체 삭제 버튼 처리 (구버전, 호환성 유지)
        if (buttonId.startsWith("delete_playlist_")) {
            String playlistName = buttonId.substring("delete_playlist_".length());
            handleDeletePlaylist(event, playlistName);
            return;
        }

        switch (buttonId) {
            case BTN_PLAY:
                handlePlay(event, musicManager, member);
                break;
            case BTN_PAUSE:
                handlePause(event, musicManager);
                break;
            case BTN_STOP:
                handleStop(event, musicManager, member);
                break;
            case BTN_SKIP:
                handleSkip(event, musicManager);
                break;
            case BTN_QUEUE:
                handleQueue(event, musicManager);
                break;
            case BTN_DB_PLAYLIST:
                handleDbPlaylist(event, member);
                break;
            case BTN_VOLUME:
                handleVolumeModal(event);
                break;
            case BTN_SHUFFLE:
                handleShuffle(event, musicManager);
                break;
            case BTN_REPEAT:
                handleRepeat(event, musicManager);
                break;
            case BTN_ADD_MUSIC:
                handleAddMusicModal(event, member);
                break;
            case BTN_CLEAR_QUEUE:
                handleClearQueue(event, musicManager);
                break;
            case BTN_REFRESH:
                handleRefresh(event, musicManager);
                break;
            case BTN_ADD_PLAYLIST:
                handleAddPlaylistModal(event);
                break;
            case BTN_EDIT_PLAYLIST:
                handleEditPlaylistMenu(event);
                break;
            default:
                event.reply("❌ 알 수 없는 버튼입니다.").setEphemeral(true).queue();
        }
    }

    /**
     * 재생 버튼 처리 (재생 시작 또는 일시정지 해제)
     */
    private void handlePlay(
            ButtonInteractionEvent event,
            GuildMusicManager musicManager,
            Member member
    ) {
        AudioPlayer player = musicManager.audioPlayer;
        TrackScheduler scheduler = musicManager.scheduler;

        // 재생 중인 곡이 있고 일시정지 상태인 경우
        if (player.getPlayingTrack() != null && player.isPaused()) {
            player.setPaused(false);
            //event.reply("▶ 재생을 재개합니다.").setEphemeral(true).queue();
            event.deferEdit().queue();
            log.info("일시정지 해제");
            return;
        }

        // 재생 중인 곡이 없는 경우 - 대기열에서 다음 곡 재생
        if (player.getPlayingTrack() == null) {
            // 음성 채널 확인
            GuildVoiceState voiceState = member.getVoiceState();
            if (voiceState == null || !voiceState.inAudioChannel()) {
                event.reply("❌ 먼저 음성 채널에 접속해주세요!").setEphemeral(true).queue();
                return;
            }

            if (musicManager.scheduler.getQueue().isEmpty()) {
                event.reply("❌ 대기열이 비어있습니다.").setEphemeral(true).queue();
                return;
            }

            // 음성 채널 연결
            Guild guild = event.getGuild();
            if (guild != null) {
                AudioChannelUnion audioChannel = voiceState.getChannel();
                AudioManager audioManager = guild.getAudioManager();

                if (!audioManager.isConnected()) {
                    audioManager.openAudioConnection(audioChannel);
                }
            }

            // 대기열에서 다음 곡 재생
            scheduler.nextTrack();
            event.deferEdit().queue();
            log.info("재생 시작 - 대기열에서 다음 곡");
            return;
        }

        // 이미 재생 중인 경우
        event.reply("⚠️ 이미 재생 중입니다.").setEphemeral(true).queue();
    }

    /**
     * 일시정지 버튼 처리
     */
    private void handlePause(ButtonInteractionEvent event, GuildMusicManager musicManager) {
        AudioPlayer player = musicManager.audioPlayer;

        if (player.getPlayingTrack() == null) {
            event.reply("❌ 재생 중인 곡이 없습니다.").setEphemeral(true).queue();
            return;
        }

        if (player.isPaused()) {
            event.reply("⚠️ 이미 일시정지 상태입니다.").setEphemeral(true).queue();
            return;
        }

        player.setPaused(true);
        event.deferEdit().queue();
        log.info("일시정지");
    }

    /**
     * 정지 버튼 처리
     */
    private void handleStop(
            ButtonInteractionEvent event,
            GuildMusicManager musicManager,
            Member member
    ) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ 서버 정보를 가져올 수 없습니다.").setEphemeral(true).queue();
            return;
        }

        musicManager.scheduler.clearQueue();
        musicManager.audioPlayer.stopTrack();

        // 음성 채널에서 나가기
        AudioManager audioManager = guild.getAudioManager();
        audioManager.closeAudioConnection();

        event.deferEdit().queue();
        log.info("음악 정지 - Guild: {}", guild.getName());
    }

    /**
     * 스킵 버튼 처리
     */
    private void handleSkip(ButtonInteractionEvent event, GuildMusicManager musicManager) {
        if (musicManager.audioPlayer.getPlayingTrack() == null) {
            event.reply("❌ 재생 중인 곡이 없습니다.").setEphemeral(true).queue();
            return;
        }

        boolean hasNext = !musicManager.scheduler.getQueue().isEmpty() || musicManager.scheduler.isRepeating();
        musicManager.scheduler.nextTrack();

        if (hasNext) {
            event.deferEdit().queue();
            log.info("스킵 실행");
        } else {
            event.reply("⏭ 다음 곡이 없습니다.").setEphemeral(true).queue();
            log.info("스킵 실행 - 대기열의 마지막 곡");
        }
    }

    /**
     * 재생목록 버튼 처리
     */
    private void handleQueue(ButtonInteractionEvent event, GuildMusicManager musicManager) {
        TrackScheduler scheduler = musicManager.scheduler;
        AudioPlayer player = musicManager.audioPlayer;

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📜 재생목록");
        embed.setColor(Color.ORANGE);

        // 현재 재생 중
        AudioTrack current = player.getPlayingTrack();
        if (current != null) {
            embed.addField(
                    "▶ 현재 재생 중",
                    String.format("**%s**", current.getInfo().title),
                    false
            );
        } else {
            embed.addField("▶ 현재 재생 중", "없음", false);
        }

        // 대기열
        BlockingQueue<AudioTrack> queue = scheduler.getQueue();
        if (queue.isEmpty()) {
            embed.addField("⏳ 대기열", "대기 중인 곡이 없습니다.", false);
        } else {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            int maxDisplay = Math.min(10, queue.size());

            for (AudioTrack track : queue) {
                if (count >= maxDisplay) break;
                sb.append(String.format(
                        "%d. **%s**\n",
                        count + 1,
                        track.getInfo().title
                ));
                count++;
            }

            if (queue.size() > maxDisplay) {
                sb.append(String.format("\n외 %d곡...", queue.size() - maxDisplay));
            }

            embed.addField("⏳ 대기열 (" + queue.size() + "곡)", sb.toString(), false);
        }

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        log.debug("재생목록 표시");
    }

    /**
     * 볼륨 조절 모달 표시
     */
    private void handleVolumeModal(ButtonInteractionEvent event) {
        TextInput volumeInput = TextInput.create(INPUT_VOLUME, "볼륨 (0-100)", TextInputStyle.SHORT)
                                         .setPlaceholder("볼륨을 입력하세요 (0-100)")
                                         .setMinLength(1)
                                         .setMaxLength(3)
                                         .setRequired(true)
                                         .build();

        Modal modal = Modal.create(MODAL_VOLUME, "🔊 볼륨 조절")
                           .addActionRow(volumeInput)
                           .build();

        event.replyModal(modal).queue();
        log.debug("볼륨 모달 표시");
    }

    /**
     * 셔플 버튼 처리
     */
    private void handleShuffle(ButtonInteractionEvent event, GuildMusicManager musicManager) {
        BlockingQueue<AudioTrack> queue = musicManager.scheduler.getQueue();

        if (queue.isEmpty()) {
            event.reply("❌ 대기열이 비어있습니다.").setEphemeral(true).queue();
            return;
        }

        List<AudioTrack> tracks = new ArrayList<>(queue);
        Collections.shuffle(tracks);

        queue.clear();
        queue.addAll(tracks);

        event.reply("🔀 대기열을 셔플했습니다!").setEphemeral(true).queue();
        log.info("대기열 셔플 완료 - {} 곡", tracks.size());
    }

    /**
     * 반복 재생 버튼 처리
     */
    private void handleRepeat(ButtonInteractionEvent event, GuildMusicManager musicManager) {
        boolean newRepeatState = !musicManager.scheduler.isRepeating();
        musicManager.scheduler.setRepeating(newRepeatState);

        String message = newRepeatState ? "🔁 반복 재생이 켜졌습니다." : "➡ 반복 재생이 꺼졌습니다.";
        event.reply(message).setEphemeral(true).queue();
        log.info("반복 재생 설정 변경 - {}", newRepeatState);
    }

    /**
     * 음악 추가 모달 표시
     */
    private void handleAddMusicModal(ButtonInteractionEvent event, Member member) {
        // 음성 채널 확인 - 서버 소유자 또는 관리자 권한이 있으면 체크 건너뛰기
        GuildVoiceState voiceState = member.getVoiceState();
        boolean isAdmin = member.isOwner() || member.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR);
        
        if ((voiceState == null || !voiceState.inAudioChannel()) && !isAdmin) {
            event.reply("❌ 먼저 음성 채널에 접속해주세요!").setEphemeral(true).queue();
            return;
        }
        
        TextInput urlInput = TextInput.create(INPUT_MUSIC_URL, "유튜브 URL 또는 검색어", TextInputStyle.SHORT)
                                      .setPlaceholder("유튜브 URL 또는 검색어를 입력하세요")
                                      .setMinLength(1)
                                      .setMaxLength(500)
                                      .setRequired(true)
                                      .build();

        Modal modal = Modal.create(MODAL_ADD_MUSIC, "➕ 음악 추가")
                           .addActionRow(urlInput)
                           .build();

        event.replyModal(modal).queue();
        log.debug("음악 추가 모달 표시");
    }

    /**
     * DB 재생목록 버튼 처리
     */
    private void handleDbPlaylist(ButtonInteractionEvent event, Member member) {
        // 재생목록 이름 목록 조회 (중복 제거)
        List<String> playlistNames = musicListService.findAllPlaylistNames();

        if (playlistNames.isEmpty()) {
            event.reply("💾 저장된 재생목록이 없습니다.").setEphemeral(true).queue();
            return;
        }

        // Embed 생성
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("💾 저장된 재생목록");
        embed.setColor(Color.MAGENTA);
        embed.setDescription("재생할 재생목록을 선택하세요!");

        // 재생목록 목록 추가
        StringBuilder sb = new StringBuilder();
        int displayLimit = Math.min(10, playlistNames.size());

        for (int i = 0; i < displayLimit; i++) {
            String playlistName = playlistNames.get(i);
            List<com.kkc.discord_bot.entity.MusicList> songsInPlaylist = musicListService.findByName(playlistName);

            sb.append(String.format("`%d.` **%s** (%d곡)\n\n", i + 1, playlistName, songsInPlaylist.size()));
        }

        if (playlistNames.size() > displayLimit) {
            sb.append(String.format("외 %d개...", playlistNames.size() - displayLimit));
        }

        embed.addField("재생목록", sb.toString(), false);
        embed.setFooter("최대 25개까지 선택 가능합니다", null);

        // SelectMenu 생성 (최대 25개)
        StringSelectMenu.Builder selectMenu = StringSelectMenu.create(SELECT_DB_PLAYLIST)
                                                              .setPlaceholder("재생목록을 선택하세요");

        int selectLimit = Math.min(25, playlistNames.size());
        for (int i = 0; i < selectLimit; i++) {
            String playlistName = playlistNames.get(i);
            List<com.kkc.discord_bot.entity.MusicList> songsInPlaylist = musicListService.findByName(playlistName);

            String label = playlistName;
            if (label.length() > 100) {
                label = label.substring(0, 97) + "...";
            }

            String description = songsInPlaylist.size() + "곡";

            selectMenu.addOption(label, playlistName, description);
        }

        // 메시지 전송
        event.replyEmbeds(embed.build())
             .addActionRow(selectMenu.build())
             .setEphemeral(true)
             .queue();

        log.info("DB 재생목록 표시 - {} 개", playlistNames.size());
    }

    /**
     * 대기열 비우기 버튼 처리
     */
    private void handleClearQueue(ButtonInteractionEvent event, GuildMusicManager musicManager) {
        int queueSize = musicManager.scheduler.getQueue().size();
        musicManager.scheduler.clearQueue();

        event.reply(String.format("🗑 대기열을 비웠습니다. (%d곡 삭제)", queueSize))
             .setEphemeral(true)
             .queue();
        log.info("대기열 비우기 - {} 곡 삭제", queueSize);
    }

    /**
     * 새로고침 버튼 처리
     */
    private void handleRefresh(ButtonInteractionEvent event, GuildMusicManager musicManager) {
        // 기존 메시지 수정
        EmbedBuilder embed = createMusicGuiEmbed(musicManager.audioPlayer, musicManager);
        List<ActionRow> buttons = createMusicGuiButtons(musicManager.audioPlayer);

        event.editMessageEmbeds(embed.build())
             .setComponents(buttons)
             .queue();

        log.debug("GUI 새로고침");
    }

    /**
     * 모달 제출 이벤트 처리
     *
     * @param event 모달 인터랙션 이벤트
     */
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();

        if (modalId.equals(MODAL_ADD_MUSIC)) {
            handleAddMusicSubmit(event);
        } else if (modalId.equals(MODAL_VOLUME)) {
            handleVolumeSubmit(event);
        } else if (modalId.equals(MODAL_ADD_PLAYLIST)) {
            handleAddPlaylistSubmit(event);
        } else if (modalId.startsWith(MODAL_ADD_SONG_TO_PLAYLIST + ":")) {
            String playlistName = modalId.substring((MODAL_ADD_SONG_TO_PLAYLIST + ":").length());
            handleAddSongToPlaylistSubmit(event, playlistName);
        }
    }

    /**
     * 음악 추가 모달 제출 처리 (URL, 검색어, 재생목록 처리)
     */
    private void handleAddMusicSubmit(ModalInteractionEvent event) {
        String input = event.getValue(INPUT_MUSIC_URL).getAsString().trim();
        Member member = event.getMember();

        if (member == null) {
            event.reply("❌ 멤버 정보를 가져올 수 없습니다.").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ 서버 정보를 가져올 수 없습니다.").setEphemeral(true).queue();
            return;
        }

        GuildVoiceState voiceState = member.getVoiceState();
        boolean isAdmin = member.isOwner() || member.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR);

        if ((voiceState == null || !voiceState.inAudioChannel()) && !isAdmin) {
            event.reply("❌ 먼저 음성 채널에 접속해주세요!").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        event.deferReply().setEphemeral(true).queue();

        AudioChannelUnion audioChannel = voiceState.getChannel();
        AudioManager audioManager = guild.getAudioManager();
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(audioChannel);
        }

        String loadingInput = input.startsWith("http") ? input : "ytsearch:" + input;
        log.info("음악 추가/검색 시작 - 입력: {}", input);

        PlayerManager.getINSTANCE().getAudioPlayerManager().loadItem(
                loadingInput, new com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        connectAndAddTrack(member, guild, channel, track);
                        event.getHook().editOriginal("✅ 음악을 추가했습니다: **" + track.getInfo().title + "**").setComponents().queue();
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        if (playlist.isSearchResult()) {
                            showSearchResults(event, member, guild, channel, playlist.getTracks(), input);
                        } else {
                            List<AudioTrack> tracks = playlist.getTracks();
                            if (tracks.isEmpty()) {
                                event.getHook().editOriginal("❌ 재생목록이 비어있습니다.").queue();
                                return;
                            }

                            EmbedBuilder embed = new EmbedBuilder();
                            embed.setTitle("🎵 대기열에 재생목록 추가");
                            embed.setColor(Color.CYAN);
                            embed.addField("재생목록", playlist.getName(), false);
                            embed.addField("총 곡 수", tracks.size() + "곡", false);
                            embed.setDescription("이 재생목록을 어떻게 추가할까요?");

                            String url = input; // The original URL
                            StringSelectMenu selectMenu = StringSelectMenu.create(SELECT_QUEUE_PLAYLIST_SHUFFLE)
                                                                          .setPlaceholder("추가 방식을 선택하세요")
                                                                          .addOption("순서대로 추가", "order:" + url)
                                                                          .addOption("랜덤으로 추가", "random:" + url)
                                                                          .build();

                            event.getHook().editOriginalEmbeds(embed.build())
                                 .setActionRow(selectMenu)
                                 .queue();
                            log.info("재생목록 추가 옵션 표시 - 이름: {}, 곡 수: {}", playlist.getName(), tracks.size());
                        }
                    }

                    @Override
                    public void noMatches() {
                        event.getHook().editOriginal("❌ 검색 결과가 없습니다.").queue();
                    }

                    @Override
                    public void loadFailed(FriendlyException exception) {
                        event.getHook().editOriginal("❌ 로드 중 오류가 발생했습니다: " + exception.getMessage()).queue();
                    }
                }
        );
    }

    /**
     * 검색 결과를 Embed와 SelectMenu로 표시
     */
    private void showSearchResults(
            ModalInteractionEvent event,
            Member member,
            Guild guild,
            TextChannel channel,
            List<AudioTrack> tracks,
            String query
    ) {
        // 상위 5개만 선택
        List<AudioTrack> topTracks = tracks.stream().limit(5).collect(Collectors.toList());

        // Embed 생성
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🔍 검색 결과: " + query);
        embed.setColor(Color.CYAN);
        embed.setDescription("아래 목록에서 재생할 곡을 선택하세요!");

        // 검색 결과 목록 추가
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < topTracks.size(); i++) {
            AudioTrack track = topTracks.get(i);
            sb.append(String.format(
                    "`%d.` **%s**\n길이: %s\n\n",
                    i + 1,
                    track.getInfo().title,
                    formatTime(track.getDuration())
            ));
        }
        embed.addField("검색된 곡들", sb.toString(), false);
        embed.setFooter("선택하지 않으면 자동으로 사라집니다", null);

        // SelectMenu 생성
        StringSelectMenu.Builder selectMenu = StringSelectMenu.create(SELECT_SEARCH_RESULT)
                                                              .setPlaceholder("재생할 곡을 선택하세요");

        for (int i = 0; i < topTracks.size(); i++) {
            AudioTrack track = topTracks.get(i);
            String label = track.getInfo().title;
            if (label.length() > 100) {
                label = label.substring(0, 97) + "...";
            }

            selectMenu.addOption(
                    label,
                    track.getInfo().uri
            );
        }

        // 메시지 전송
        event.getHook().editOriginalEmbeds(embed.build())
             .setActionRow(selectMenu.build())
             .queue();

        log.info("검색 결과 표시 완료 - {} 곡", topTracks.size());
    }

    /**
     * 음성 채널 연결 및 트랙 추가
     */
    private void connectAndAddTrack(
            Member member,
            Guild guild,
            TextChannel channel,
            AudioTrack track
    ) {
        GuildVoiceState voiceState = member.getVoiceState();
        AudioChannelUnion audioChannel = voiceState.getChannel();
        AudioManager audioManager = guild.getAudioManager();

        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(audioChannel);
        }

        GuildMusicManager musicManager = PlayerManager.getINSTANCE().getMusicManager(guild, channel);
        musicManager.scheduler.queueAndPlay(track);
        musicManager.scheduler.checkAndStartAloneTimer();

        log.info("트랙 추가 완료: {}", track.getInfo().title);
    }

    /**
     * StringSelectMenu 이벤트 처리
     */
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.equals(SELECT_SEARCH_RESULT)) {
            handleSearchResultSelection(event);
        } else if (componentId.equals(SELECT_DB_PLAYLIST)) {
            handleDbPlaylistSelection(event);
        } else if (componentId.equals(SELECT_EDIT_PLAYLIST)) {
            handleEditPlaylistSelection(event);
        } else if (componentId.equals(SELECT_PLAYLIST_SHUFFLE)) {
            handlePlaylistShuffleSelection(event);
        } else if (componentId.startsWith(SELECT_DB_PLAYLIST_SHUFFLE_CHOICE)) {
            handleDbPlaylistShuffleChoice(event);
        } else if (componentId.equals(SELECT_QUEUE_PLAYLIST_SHUFFLE)) {
            handleQueuePlaylistShuffleSelection(event);
        }
    }

    private void handleQueuePlaylistShuffleSelection(StringSelectInteractionEvent event) {
        String selectedValue = event.getValues().get(0);
        String[] parts = selectedValue.split(":", 2);

        if (parts.length != 2) {
            event.reply("❌ 잘못된 선택입니다.").setEphemeral(true).queue();
            return;
        }

        String mode = parts[0];
        String playlistUrl = parts[1];
        boolean shouldShuffle = mode.equals("random");

        Member member = event.getMember();
        Guild guild = event.getGuild();
        TextChannel channel = event.getChannel().asTextChannel();

        if (member == null || guild == null) {
            event.reply("❌ 오류가 발생했습니다.").setEphemeral(true).queue();
            return;
        }

        event.deferEdit().queue();

        PlayerManager.getINSTANCE().getAudioPlayerManager().loadItem(
                playlistUrl, new com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        event.getHook().editOriginal("❌ 오류: 재생목록을 로드해야 합니다.").setComponents().queue();
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        List<AudioTrack> tracks = new ArrayList<>(playlist.getTracks());
                        if (tracks.isEmpty()) {
                            event.getHook().editOriginal("❌ 재생목록이 비어있습니다.").setComponents().queue();
                            return;
                        }

                        if (shouldShuffle) {
                            Collections.shuffle(tracks);
                        }

                        GuildMusicManager musicManager = PlayerManager.getINSTANCE().getMusicManager(guild, channel);
                        for (AudioTrack t : tracks) {
                            musicManager.scheduler.queue(t);
                        }

                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setTitle("✅ 대기열에 추가 완료");
                        embed.setColor(Color.GREEN);
                        embed.addField("추가된 재생목록", playlist.getName(), false);
                        embed.addField("곡 수", tracks.size() + "곡", true);
                        embed.addField("추가 방식", shouldShuffle ? "랜덤" : "순서대로", true);

                        event.getHook().editOriginalEmbeds(embed.build())
                             .setComponents()
                             .queue();

                        log.info("대기열에 재생목록 추가 완료 - {} 곡, 랜덤: {}", tracks.size(), shouldShuffle);
                    }

                    @Override
                    public void noMatches() {
                        event.getHook().editOriginal("❌ 재생목록을 찾을 수 없습니다.").setComponents().queue();
                    }

                    @Override
                    public void loadFailed(FriendlyException exception) {
                        event.getHook().editOriginal("❌ 재생목록 로드 실패: " + exception.getMessage()).setComponents().queue();
                    }
                }
        );
    }

    /**
     * 검색 결과 선택 처리
     */
    private void handleSearchResultSelection(StringSelectInteractionEvent event) {
        String selectedUrl = event.getValues().get(0);
        Member member = event.getMember();

        if (member == null) {
            event.reply("❌ 멤버 정보를 가져올 수 없습니다.").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ 서버 정보를 가져올 수 없습니다.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();

        // 선택한 곡 로드
        event.deferEdit().queue();
        loadSelectedTrack(event, member, guild, channel, selectedUrl);

        //임베드 삭제
        event.getHook().deleteOriginal().queue();
    }

    /**
     * DB 재생목록 선택 처리
     */
    private void handleDbPlaylistSelection(StringSelectInteractionEvent event) {
        String playlistName = event.getValues().get(0);
        Member member = event.getMember();

        if (member == null) {
            event.reply("❌ 멤버 정보를 가져올 수 없습니다.").setEphemeral(true).queue();
            return;
        }

        // 음성 채널 확인
        GuildVoiceState voiceState = member.getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply("❌ 먼저 음성 채널에 접속해주세요!").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🎲 랜덤 재생 설정");
        embed.setDescription("`" + playlistName + "` 재생목록을 어떻게 재생할까요?");
        embed.setColor(Color.CYAN);

        StringSelectMenu shuffleMenu = StringSelectMenu.create(SELECT_DB_PLAYLIST_SHUFFLE_CHOICE + ":" + playlistName)
                                                       .setPlaceholder("재생 방식을 선택하세요")
                                                       .addOption("순서대로 재생", "order", "원래 순서대로 재생합니다.")
                                                       .addOption("랜덤으로 재생", "random", "순서를 섞어서 재생합니다.")
                                                       .build();

        event.replyEmbeds(embed.build())
             .addActionRow(shuffleMenu)
             .setEphemeral(true)
             .queue();
    }

    private void handleDbPlaylistShuffleChoice(StringSelectInteractionEvent event) {
        String selectedValue = event.getValues().get(0);
        String playlistName = event.getComponentId().split(":")[1];
        boolean shuffle = selectedValue.equals("random");

        Member member = event.getMember();
        Guild guild = event.getGuild();
        TextChannel channel = event.getChannel().asTextChannel();

        if (member == null || guild == null) {
            event.reply("❌ 오류가 발생했습니다.").setEphemeral(true).queue();
            return;
        }

        // 음성 채널 연결
        GuildVoiceState voiceState = member.getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply("❌ 먼저 음성 채널에 접속해주세요!").setEphemeral(true).queue();
            return;
        }
        AudioChannelUnion audioChannel = voiceState.getChannel();
        AudioManager audioManager = guild.getAudioManager();
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(audioChannel);
        }

        // DB에서 곡 목록 가져오기
        List<com.kkc.discord_bot.entity.MusicList> songs = musicListService.findByName(playlistName);
        if (songs.isEmpty()) {
            event.reply("❌ 재생목록에 곡이 없습니다.").setEphemeral(true).queue();
            return;
        }

        // 셔플이 선택된 경우 목록 섞기
        if (shuffle) {
            Collections.shuffle(songs);
        }

        // 재생목록 로드
        event.deferEdit().queue(); // 중요: 이전 메시지를 수정하기 위해 필요
        loadDbPlaylist(event, member, guild, channel, playlistName, songs, shuffle);
    }

    /**
     * DB 재생목록 로드 및 재생 (이름 기반)
     */
    private void loadDbPlaylist(
            StringSelectInteractionEvent event,
            Member member,
            Guild guild,
            TextChannel channel,
            String playlistName,
            List<com.kkc.discord_bot.entity.MusicList> songs,
            boolean shuffled
    ) {
        log.info("DB 재생목록 로드 시작 - 이름: {}, 곡 수: {}, 셔플: {}", playlistName, songs.size(), shuffled);

        // 각 곡의 URL을 로드하여 대기열에 추가
        for (com.kkc.discord_bot.entity.MusicList song : songs) {
            String url = song.getUrl();
            PlayerManager.getINSTANCE().loadAndPlay(channel, url, member);
        }

        // 완료 메시지
        EmbedBuilder successEmbed = new EmbedBuilder();
        successEmbed.setTitle("✅ 재생목록 추가 완료");
        successEmbed.setColor(Color.GREEN);
        successEmbed.addField("재생목록", "**" + playlistName + "**", false);
        successEmbed.addField("곡 수", songs.size() + "곡", true);
        successEmbed.addField("재생 방식", shuffled ? "랜덤" : "순차", true);
        successEmbed.setDescription("재생목록의 모든 곡이 대기열에 추가되었습니다!");

        event.getHook().editOriginalEmbeds(successEmbed.build())
             .setComponents() // 선택 메뉴 제거
             .queue();

        log.info("DB 재생목록 로드 완료 - {} ({} 곡)", playlistName, songs.size());
    }

    /**
     * 선택한 트랙 로드 및 추가
     */
    private void loadSelectedTrack(
            StringSelectInteractionEvent event,
            Member member,
            Guild guild,
            TextChannel channel,
            String trackUrl
    ) {
        GuildMusicManager musicManager = PlayerManager.getINSTANCE().getMusicManager(guild, channel);

        PlayerManager.getINSTANCE().getAudioPlayerManager().loadItem(
                trackUrl, new com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        connectAndAddTrack(member, guild, channel, track);

                        // 기존 메시지 수정
 /*                       EmbedBuilder successEmbed = new EmbedBuilder();
                        successEmbed.setTitle("✅ 음악 추가 완료");
                        successEmbed.setColor(Color.GREEN);
                        successEmbed.addField("재생 곡", "**" + track.getInfo().title + "**", false);
                        successEmbed.addField("작곡가", track.getInfo().author, true);
                        successEmbed.addField("길이", formatTime(track.getDuration()), true);

                        event.getHook().editOriginalEmbeds(successEmbed.build())
                             .setComponents() // 버튼 제거
                             .queue();
*/
                        log.info("선택한 트랙 추가 완료 - {}", track.getInfo().title);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        if (!playlist.getTracks().isEmpty()) {
                            AudioTrack track = playlist.getTracks().get(0);
                            connectAndAddTrack(member, guild, channel, track);
                            event.getHook().editOriginal("✅ 음악을 추가했습니다: **" + track.getInfo().title + "**")
                                 .setComponents()
                                 .queue();
                        }
                    }

                    @Override
                    public void noMatches() {
                        event.getHook().editOriginal("❌ 음악을 찾을 수 없습니다.")
                             .setComponents()
                             .queue();
                    }

                    @Override
                    public void loadFailed(FriendlyException exception) {
                        event.getHook().editOriginal("❌ 음악 로드 실패: " + exception.getMessage())
                             .setComponents()
                             .queue();
                    }
                }
        );
    }

    /**
     * 볼륨 조절 모달 제출 처리
     */
    private void handleVolumeSubmit(ModalInteractionEvent event) {
        String volumeStr = event.getValue(INPUT_VOLUME).getAsString().trim();

        try {
            int volume = Integer.parseInt(volumeStr);

            if (volume < 0 || volume > 100) {
                event.reply("❌ 볼륨은 0에서 100 사이의 값이어야 합니다.").setEphemeral(true).queue();
                return;
            }

            Guild guild = event.getGuild();
            if (guild == null) {
                event.reply("❌ 서버 정보를 가져올 수 없습니다.").setEphemeral(true).queue();
                return;
            }

            TextChannel channel = event.getChannel().asTextChannel();
            GuildMusicManager musicManager = PlayerManager.getINSTANCE().getMusicManager(guild, channel);
            musicManager.audioPlayer.setVolume(volume);

            event.reply(String.format("🔊 볼륨을 %d%%로 설정했습니다.", volume)).setEphemeral(true).queue();
            log.info("볼륨 변경 - {}%", volume);

        } catch (NumberFormatException e) {
            event.reply("❌ 올바른 숫자를 입력해주세요.").setEphemeral(true).queue();
        }
    }

    /**
     * 재생목록 추가 모달 표시
     */
    private void handleAddPlaylistModal(ButtonInteractionEvent event) {
        TextInput urlInput = TextInput.create(INPUT_PLAYLIST_URL, "유튜브 URL (재생목록 또는 단일 곡)", TextInputStyle.SHORT)
                                      .setPlaceholder("재생목록 URL 또는 단일 곡 URL을 입력하세요")
                                      .setMinLength(1)
                                      .setMaxLength(500)
                                      .setRequired(true)
                                      .build();

        TextInput nameInput = TextInput.create(INPUT_PLAYLIST_NAME, "재생목록 이름", TextInputStyle.SHORT)
                                       .setPlaceholder("저장할 재생목록 이름을 입력하세요")
                                       .setMinLength(1)
                                       .setMaxLength(100)
                                       .setRequired(true)
                                       .build();

        Modal modal = Modal.create(MODAL_ADD_PLAYLIST, "➕ 재생목록 추가")
                           .addActionRow(urlInput)
                           .addActionRow(nameInput)
                           .build();

        event.replyModal(modal).queue();
        log.debug("재생목록 추가 모달 표시");
    }

    /**
     * 재생목록 편집 메뉴 표시
     */
    private void handleEditPlaylistMenu(ButtonInteractionEvent event) {
        List<String> playlistNames = musicListService.findAllPlaylistNames();

        if (playlistNames.isEmpty()) {
            event.reply("💾 저장된 재생목록이 없습니다.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("✏️ 재생목록 편집");
        embed.setColor(Color.ORANGE);
        embed.setDescription("편집할 재생목록을 선택하세요!");

        // SelectMenu 생성
        StringSelectMenu.Builder selectMenu = StringSelectMenu.create(SELECT_EDIT_PLAYLIST)
                                                              .setPlaceholder("편집할 재생목록을 선택하세요");

        int selectLimit = Math.min(25, playlistNames.size());
        for (int i = 0; i < selectLimit; i++) {
            String playlistName = playlistNames.get(i);
            List<com.kkc.discord_bot.entity.MusicList> songsInPlaylist = musicListService.findByName(playlistName);

            String label = playlistName;
            if (label.length() > 100) {
                label = label.substring(0, 97) + "...";
            }

            String description = songsInPlaylist.size() + "곡";

            selectMenu.addOption(label, playlistName, description);
        }

        event.replyEmbeds(embed.build())
             .addActionRow(selectMenu.build())
             .setEphemeral(true)
             .queue();

        log.debug("재생목록 편집 메뉴 표시");
    }

    /**
     * 재생목록 전체 삭제 처리
     */
    private void handleDeletePlaylist(ButtonInteractionEvent event, String playlistName) {
        try {
            // 해당 이름의 모든 노래 삭제
            int deletedCount = musicListService.deleteByName(playlistName);

            if (deletedCount == 0) {
                event.reply("❌ 재생목록을 찾을 수 없습니다.").setEphemeral(true).queue();
                return;
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🗑️ 재생목록 삭제 완료");
            embed.setColor(Color.RED);
            embed.addField("삭제된 재생목록", playlistName, false);
            embed.addField("삭제된 곡 수", deletedCount + "곡", false);

            event.editMessageEmbeds(embed.build()).setComponents().queue();

            log.info("재생목록 전체 삭제 완료 - 이름: {}, 곡 수: {}", playlistName, deletedCount);
        } catch (Exception e) {
            log.error("재생목록 삭제 중 오류 - 이름: {}", playlistName, e);
            event.reply("❌ 재생목록 삭제 중 오류가 발생했습니다: " + e.getMessage())
                 .setEphemeral(true)
                 .queue();
        }
    }

    /**
     * 재생목록 추가 모달 제출 처리
     * 전체 곡을 추가하고 랜덤 여부를 선택하는 셀렉트 메뉴를 표시합니다.
     */
    private void handleAddPlaylistSubmit(ModalInteractionEvent event) {
        String playlistUrl = event.getValue(INPUT_PLAYLIST_URL).getAsString().trim();
        String playlistName = event.getValue(INPUT_PLAYLIST_NAME).getAsString().trim();

        // URL 검증
        if (!playlistUrl.contains("youtube.com") && !playlistUrl.contains("youtu.be")) {
            event.reply("❌ 유튜브 URL만 지원됩니다.").setEphemeral(true).queue();
            return;
        }

        event.deferReply().setEphemeral(true).queue();

        // 재생목록 로드
        PlayerManager.getINSTANCE().getAudioPlayerManager().loadItem(
                playlistUrl, new com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        // 단일 트랙 저장
                        try {
                            com.kkc.discord_bot.entity.MusicList musicList = new com.kkc.discord_bot.entity.MusicList();
                            musicList.setName(playlistName);
                            musicList.setUrl(track.getInfo().uri);
                            musicList.setTitle(track.getInfo().title);
                            musicList.setAuthor(track.getInfo().author);

                            boolean saved = musicListService.save(musicList);

                            if (saved) {
                                EmbedBuilder embed = new EmbedBuilder();
                                embed.setTitle("✅ 곡 추가 완료");
                                embed.setColor(Color.GREEN);
                                embed.addField("재생목록", playlistName, false);
                                embed.addField("추가된 곡", String.format("**%s**", track.getInfo().title), false);
                                event.getHook().editOriginalEmbeds(embed.build()).setComponents().queue();
                                log.info("단일 곡 저장 완료 - 재생목록: {}, 곡: {}", playlistName, track.getInfo().title);
                            } else {
                                event.getHook().editOriginal("⚠️ 이미 재생목록에 존재하는 곡입니다.").queue();
                            }
                        } catch (Exception e) {
                            event.getHook().editOriginal("❌ 곡 추가 중 오류가 발생했습니다: " + e.getMessage()).queue();
                            log.error("단일 곡 저장 중 오류", e);
                        }
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        List<AudioTrack> tracks = playlist.getTracks();

                        if (tracks.isEmpty()) {
                            event.getHook().editOriginal("❌ 재생목록이 비어있습니다.").queue();
                            return;
                        }

                        // 랜덤 추가 여부 선택 메뉴 표시
                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setTitle("🎵 재생목록 추가 옵션");
                        embed.setColor(Color.CYAN);
                        embed.addField("재생목록 이름", playlistName, false);
                        embed.addField("총 곡 수", tracks.size() + "곡", false);
                        embed.setDescription("재생목록을 어떻게 추가할까요?");

                        StringSelectMenu selectMenu = StringSelectMenu.create(SELECT_PLAYLIST_SHUFFLE)
                                                                      .setPlaceholder("추가 방식을 선택하세요")
                                                                      .addOption("순서대로 추가", "order:" + playlistName + ":" + playlistUrl, "재생목록의 원래 순서대로 추가합니다")
                                                                      .addOption("랜덤으로 추가", "random:" + playlistName + ":" + playlistUrl, "재생목록을 랜덤 순서로 추가합니다")
                                                                      .build();

                        event.getHook().editOriginalEmbeds(embed.build())
                             .setActionRow(selectMenu)
                             .queue();

                        log.info("재생목록 추가 옵션 표시 - 이름: {}, 곡 수: {}", playlistName, tracks.size());
                    }

                    @Override
                    public void noMatches() {
                        event.getHook().editOriginal("❌ URL을 찾을 수 없습니다.").queue();
                    }

                    @Override
                    public void loadFailed(FriendlyException exception) {
                        event.getHook().editOriginal("❌ URL 로드 실패: " + exception.getMessage()).queue();
                        log.error("URL 로드 실패", exception);
                    }
                }
        );
    }

    /**
     * 재생목록 셔플 선택 처리
     */
    private void handlePlaylistShuffleSelection(StringSelectInteractionEvent event) {
        String selectedValue = event.getValues().get(0);
        String[] parts = selectedValue.split(":", 3);

        if (parts.length != 3) {
            event.reply("❌ 잘못된 선택입니다.").setEphemeral(true).queue();
            return;
        }

        String mode = parts[0]; // "order" or "random"
        String playlistName = parts[1];
        String playlistUrl = parts[2];

        boolean shouldShuffle = mode.equals("random");

        event.deferEdit().queue();

        // 재생목록 로드 및 저장
        PlayerManager.getINSTANCE().getAudioPlayerManager().loadItem(
                playlistUrl, new com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        event.getHook().editOriginal("❌ 재생목록 URL이 아닙니다.").queue();
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        List<AudioTrack> tracks = new ArrayList<>(playlist.getTracks());

                        if (tracks.isEmpty()) {
                            event.getHook().editOriginal("❌ 재생목록이 비어있습니다.").queue();
                            return;
                        }

                        // 랜덤 모드인 경우 섞기
                        if (shouldShuffle) {
                            Collections.shuffle(tracks);
                            log.info("재생목록 랜덤 모드 - 곡 순서를 섞었습니다");
                        }

                        // DB에 저장
                        int savedCount = 0;
                        for (AudioTrack track : tracks) {
                            try {
                                com.kkc.discord_bot.entity.MusicList musicList = new com.kkc.discord_bot.entity.MusicList();
                                musicList.setName(playlistName);
                                musicList.setUrl(track.getInfo().uri);
                                musicList.setTitle(track.getInfo().title);
                                musicList.setAuthor(track.getInfo().author);

                                if (musicListService.save(musicList)) {
                                    savedCount++;
                                }
                            } catch (Exception e) {
                                log.error("재생목록 저장 중 오류 - 트랙: {}", track.getInfo().title, e);
                            }
                        }

                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setTitle("✅ 재생목록 저장 완료");
                        embed.setColor(Color.GREEN);
                        embed.addField("재생목록 이름", playlistName, false);
                        embed.addField("저장 방식", shouldShuffle ? "랜덤 순서로 저장" : "순서대로 저장", false);
                        embed.addField("저장된 곡 수", savedCount + "곡 (중복 제외)", false);
                        embed.addField("전체 곡 수", tracks.size() + "곡", false);

                        event.getHook().editOriginalEmbeds(embed.build())
                             .setComponents() // 셀렉트 메뉴 제거
                             .queue();

                        log.info("재생목록 저장 완료 - 이름: {}, 곡 수: {}, 랜덤: {}", playlistName, savedCount, shouldShuffle);
                    }

                    @Override
                    public void noMatches() {
                        event.getHook().editOriginal("❌ 재생목록을 찾을 수 없습니다.").queue();
                    }

                    @Override
                    public void loadFailed(FriendlyException exception) {
                        event.getHook().editOriginal("❌ 재생목록 로드 실패: " + exception.getMessage()).queue();
                        log.error("재생목록 로드 실패", exception);
                    }
                }
        );
    }

    /**
     * 편집할 재생목록 선택 처리 (개선됨)
     */
    private void handleEditPlaylistSelection(StringSelectInteractionEvent event) {
        String playlistName = event.getValues().get(0);
        showEditablePlaylistPage(event, playlistName, 0);
        log.info("재생목록 편집 화면 표시 - {}", playlistName);
    }

    private void showEditablePlaylistPage(
            ComponentInteraction event,
            String playlistName,
            int page
    ) {
        List<com.kkc.discord_bot.entity.MusicList> songs = musicListService.findByName(playlistName);

        final int songsPerPage = 5;
        int totalPages = (songs.isEmpty()) ? 1 : (int) Math.ceil((double) songs.size() / songsPerPage);
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("✏️ 재생목록 편집: " + playlistName);
        embed.setColor(Color.ORANGE);
        embed.setFooter("페이지 " + (currentPage + 1) + "/" + totalPages + " | 총 " + songs.size() + "곡");

        List<ActionRow> actionRows = new ArrayList<>();

        if (songs.isEmpty()) {
            embed.setDescription("노래가 없습니다. 아래 '곡 추가' 버튼으로 노래를 추가해보세요.");
        } else {
            StringBuilder sb = new StringBuilder();
            int startIndex = currentPage * songsPerPage;
            int endIndex = Math.min(startIndex + songsPerPage, songs.size());

            List<Button> deleteButtons = new ArrayList<>();
            for (int i = startIndex; i < endIndex; i++) {
                com.kkc.discord_bot.entity.MusicList song = songs.get(i);
                sb.append(String.format("`%d.` **%s**\n", i + 1, song.getTitle()));
                // 각 노래 옆에 삭제 버튼 추가
                deleteButtons.add(Button.danger(BTN_PLAYLIST_DELETE_SONG + ":" + song.getId() + ":" + playlistName + ":" + currentPage, "삭제 " + (i + 1)));
            }
            embed.setDescription(sb.toString());
            if (!deleteButtons.isEmpty()) {
                actionRows.add(ActionRow.of(deleteButtons));
            }
        }

        Button prevButton = Button.secondary(BTN_PLAYLIST_EDIT_PREV + ":" + playlistName + ":" + currentPage, "이전").withDisabled(currentPage == 0);
        Button nextButton = Button.secondary(BTN_PLAYLIST_EDIT_NEXT + ":" + playlistName + ":" + currentPage, "다음").withDisabled(currentPage >= totalPages - 1);
        Button addSongButton = Button.success(BTN_ADD_SONG_TO_PLAYLIST + ":" + playlistName, "곡 추가");
        Button deleteAllButton = Button.danger(BTN_DELETE_ALL_PLAYLIST + ":" + playlistName, "전체 삭제").withDisabled(songs.isEmpty());
        Button closeButton = Button.secondary(BTN_CLOSE_PLAYLIST_EDIT, "닫기");


        List<Button> paginationButtons = new ArrayList<>();
        paginationButtons.add(prevButton);
        paginationButtons.add(nextButton);
        paginationButtons.add(addSongButton);
        paginationButtons.add(deleteAllButton);
        paginationButtons.add(closeButton);

        actionRows.add(ActionRow.of(paginationButtons));

        event.editMessageEmbeds(embed.build()).setComponents(actionRows).queue();
    }

    private void handleDeleteSongFromPlaylist(ButtonInteractionEvent event, String buttonId) {
        String[] parts = buttonId.split(":");
        long songId = Long.parseLong(parts[1]);
        String playlistName = parts[2];
        int page = Integer.parseInt(parts[3]);

        musicListService.deleteById(songId);

        // Refresh the view
        showEditablePlaylistPage(event, playlistName, page);
    }

    private void handlePlaylistEditPrev(ButtonInteractionEvent event, String buttonId) {
        String[] parts = buttonId.split(":");
        String playlistName = parts[1];
        int currentPage = Integer.parseInt(parts[2]);
        showEditablePlaylistPage(event, playlistName, currentPage - 1);
    }

    private void handlePlaylistEditNext(ButtonInteractionEvent event, String buttonId) {
        String[] parts = buttonId.split(":");
        String playlistName = parts[1];
        int currentPage = Integer.parseInt(parts[2]);
        showEditablePlaylistPage(event, playlistName, currentPage + 1);
    }

    /**
     * 재생목록에 곡 추가 모달 표시
     */
    private void handleAddSongToPlaylistModal(
            ButtonInteractionEvent event,
            String playlistName
    ) {
        TextInput urlInput = TextInput.create(INPUT_SONG_URL, "유튜브 URL 또는 검색어", TextInputStyle.SHORT)
                                      .setPlaceholder("추가할 곡의 유튜브 URL 또는 검색어를 입력하세요")
                                      .setMinLength(1)
                                      .setMaxLength(500)
                                      .setRequired(true)
                                      .build();

        Modal modal = Modal.create(MODAL_ADD_SONG_TO_PLAYLIST + ":" + playlistName, "➕ 곡 추가: " + playlistName)
                           .addActionRow(urlInput)
                           .build();

        event.replyModal(modal).queue();
        log.debug("곡 추가 모달 표시 - 재생목록: {}", playlistName);
    }

    /**
     * 재생목록에 곡 추가 모달 제출 처리
     */
    private void handleAddSongToPlaylistSubmit(ModalInteractionEvent event, String playlistName) {
        String input = event.getValue(INPUT_SONG_URL).getAsString().trim();

        event.deferReply().setEphemeral(true).queue();

        // URL인 경우 바로 추가
        if (input.startsWith("http")) {
            addSongToPlaylistByUrl(event, playlistName, input);
            return;
        }

        // 검색어인 경우 검색
        String searchQuery = "ytsearch:" + input;
        PlayerManager.getINSTANCE().getAudioPlayerManager().loadItem(
                searchQuery, new com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        addTrackToPlaylist(event, playlistName, track);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        List<AudioTrack> tracks = playlist.getTracks();
                        if (tracks.isEmpty()) {
                            event.getHook().editOriginal("❌ 검색 결과가 없습니다.").queue();
                            return;
                        }

                        // 첫 번째 결과 추가
                        AudioTrack firstTrack = tracks.get(0);
                        addTrackToPlaylist(event, playlistName, firstTrack);
                    }

                    @Override
                    public void noMatches() {
                        event.getHook().editOriginal("❌ 검색 결과가 없습니다.").queue();
                    }

                    @Override
                    public void loadFailed(FriendlyException exception) {
                        event.getHook().editOriginal("❌ 곡 검색 실패: " + exception.getMessage()).queue();
                        log.error("곡 검색 실패", exception);
                    }
                }
        );
    }

    /**
     * URL로 곡을 재생목록에 추가
     */
    private void addSongToPlaylistByUrl(
            ModalInteractionEvent event,
            String playlistName,
            String url
    ) {
        PlayerManager.getINSTANCE().getAudioPlayerManager().loadItem(
                url, new com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        addTrackToPlaylist(event, playlistName, track);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        event.getHook().editOriginal("❌ 단일 곡 URL만 추가 가능합니다. 재생목록 URL은 사용할 수 없습니다.").queue();
                    }

                    @Override
                    public void noMatches() {
                        event.getHook().editOriginal("❌ 곡을 찾을 수 없습니다.").queue();
                    }

                    @Override
                    public void loadFailed(FriendlyException exception) {
                        event.getHook().editOriginal("❌ 곡 로드 실패: " + exception.getMessage()).queue();
                        log.error("곡 로드 실패", exception);
                    }
                }
        );
    }

    /**
     * 트랙을 재생목록에 추가
     */
    private void addTrackToPlaylist(
            ModalInteractionEvent event,
            String playlistName,
            AudioTrack track
    ) {
        try {
            com.kkc.discord_bot.entity.MusicList musicList = new com.kkc.discord_bot.entity.MusicList();
            musicList.setName(playlistName);
            musicList.setUrl(track.getInfo().uri);
            musicList.setTitle(track.getInfo().title);
            musicList.setAuthor(track.getInfo().author);

            boolean saved = musicListService.save(musicList);

            if (saved) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("✅ 곡 추가 완료");
                embed.setColor(Color.GREEN);
                embed.addField("재생목록", playlistName, false);
                embed.addField("추가된 곡", String.format("**%s**\n작곡가: %s", track.getInfo().title, track.getInfo().author), false);

                event.getHook().editOriginalEmbeds(embed.build()).queue();
                log.info("곡 추가 완료 - 재생목록: {}, 곡: {}", playlistName, track.getInfo().title);
            } else {
                event.getHook().editOriginal("⚠️ 이미 재생목록에 존재하는 곡입니다.").queue();
            }
        } catch (Exception e) {
            event.getHook().editOriginal("❌ 곡 추가 중 오류가 발생했습니다: " + e.getMessage()).queue();
            log.error("곡 추가 중 오류", e);
        }
    }

    /**
     * 재생목록 전체 삭제 확인
     */
    private void handleConfirmDeletePlaylist(
            ComponentInteraction event,
            String playlistName
    ) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("⚠️ 재생목록 삭제 확인");
        embed.setColor(Color.RED);
        embed.addField("재생목록 이름", playlistName, false);
        embed.setDescription("정말로 이 재생목록을 완전히 삭제하시겠습니까?\n**이 작업은 되돌릴 수 없습니다!**");

        Button confirmBtn = Button.danger("confirm_delete_playlist:" + playlistName, "✅ 삭제 확인");
        Button cancelBtn = Button.secondary("cancel_delete_playlist", "❌ 취소");

        event.editMessageEmbeds(embed.build()).setActionRow(confirmBtn, cancelBtn).queue();

        log.debug("재생목록 삭제 확인 표시 - {}", playlistName);
    }
}
