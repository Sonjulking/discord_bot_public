// 생성됨 - 2025-11-03
package com.kkc.discord_bot.handler;

import com.kkc.discord_bot.entity.MusicList;
import com.kkc.discord_bot.music.PlayerManager;
import com.kkc.discord_bot.service.MusicListService;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;

/**
 * 플레이리스트 저장/로드 처리 핸들러
 *
 * 플레이리스트를 DB에 저장하고 불러오는 기능을 담당합니다.
 *
 * @author KKC
 * @since 2025-11-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaylistCommandHandler {

    private final UserInputStateManager stateManager;
    private final MusicListService musicListService;

    /**
     * 플레이리스트 이름 입력 처리
     *
     * @param event 메시지 이벤트
     * @param name 플레이리스트 이름
     */
    public void handlePlaylistNameInput(MessageReceivedEvent event, String name) {
        String authorId = event.getAuthor().getId();
        String trackURL = stateManager.removePendingPlaylistNameInput(authorId);

        if (name.equalsIgnoreCase("c")) {
            return;
        }

        if (name.trim().isEmpty()) {
            stateManager.rePutPendingPlaylistNameInput(authorId, trackURL);
            return;
        }

        TextChannel textChannel = event.getChannel().asTextChannel();

        // URL에서 list 파라미터만 추출하여 재생목록 전체를 로드
        String finalURL = trackURL;
        if (trackURL.contains("&list=") || trackURL.contains("?list=")) {
            String listId = extractPlaylistId(trackURL);
            if (listId != null) {
                finalURL = "https://www.youtube.com/playlist?list=" + listId;
            }
        }

        String urlToLoad = finalURL;
        PlayerManager.getINSTANCE().getAudioPlayerManager().loadItem(
                urlToLoad, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        saveSingleTrack(textChannel, track, name.trim());
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        savePlaylist(textChannel, playlist, name.trim());
                    }

                    @Override
                    public void noMatches() {
                        log.warn("음악을 찾을 수 없음: {}", urlToLoad);
                    }

                    @Override
                    public void loadFailed(FriendlyException exception) {
                        log.error("음악 로드 실패: {}", urlToLoad, exception);
                    }
                }
        );
    }

    /**
     * YouTube URL에서 재생목록 ID 추출
     *
     * @param url YouTube URL
     * @return 재생목록 ID 또는 null
     */
    private String extractPlaylistId(String url) {
        try {
            String[] parts = url.split("[?&]");
            for (String part : parts) {
                if (part.startsWith("list=")) {
                    return part.substring(5);
                }
            }
        } catch (Exception e) {
            log.warn("재생목록 ID 추출 실패: {}", url, e);
        }
        return null;
    }

    /**
     * 단일 트랙 저장
     *
     * @param channel 텍스트 채널
     * @param track 트랙
     * @param playlistName 플레이리스트 이름
     */
    private void saveSingleTrack(TextChannel channel, AudioTrack track, String playlistName) {
        MusicList musicList = new MusicList();
        musicList.setTitle(track.getInfo().title + " - " + track.getInfo().author);
        musicList.setUrl(track.getInfo().uri);
        musicList.setName(playlistName);

        if (musicListService.save(musicList)) {
            log.info("음악 저장 완료: {} -> {}", track.getInfo().title, playlistName);
        } else {
            log.info("음악 중복: {} -> {}", track.getInfo().title, playlistName);
        }
    }

    /**
     * 플레이리스트 저장
     *
     * @param channel 텍스트 채널
     * @param playlist 플레이리스트
     * @param playlistName 플레이리스트 이름
     */
    private void savePlaylist(TextChannel channel, AudioPlaylist playlist, String playlistName) {
        List<AudioTrack> tracks = playlist.getTracks();
        if (tracks.isEmpty()) {
            return;
        }

        int savedCount = 0;
        int skippedCount = 0;

        for (AudioTrack track : tracks) {
            MusicList musicList = new MusicList();
            musicList.setTitle(track.getInfo().title + " - " + track.getInfo().author);
            musicList.setUrl(track.getInfo().uri);
            musicList.setName(playlistName);

            if (musicListService.save(musicList)) {
                savedCount++;
            } else {
                skippedCount++;
            }
        }

        log.info("플레이리스트 저장 완료: {} - {}곡 저장, {}곡 중복", playlistName, savedCount, skippedCount);
    }

    /**
     * 플레이리스트 저장 시작
     *
     * @param event 메시지 이벤트
     * @param trackURL 트랙 URL
     */
    public void saveMusicList(MessageReceivedEvent event, String trackURL) {
        if (trackURL.isEmpty()) {
            return;
        }

        if (stateManager.hasPendingPlaylistNameInput(event.getAuthor().getId())) {
            return;
        }

        stateManager.setPendingPlaylistNameInput(event.getAuthor().getId(), trackURL);
    }

    /**
     * 저장된 플레이리스트 랜덤 재생 선택 처리
     *
     * @param event 메시지 이벤트
     * @param msg 사용자 입력
     */
    public void handleSavedPlaylistRandomChoice(MessageReceivedEvent event, String msg) {
        String authorId = event.getAuthor().getId();
        String playlistName = stateManager.removePendingSavedPlaylistRandomChoice(authorId);
        String choice = msg.toLowerCase();

        if (choice.equals("y") || choice.equals("yes")) {
            playSavedMusicListWithShuffle(event, playlistName, true);
        } else if (choice.equals("n") || choice.equals("no")) {
            playSavedMusicListWithShuffle(event, playlistName, false);
        } else {
            stateManager.rePutPendingSavedPlaylistRandomChoice(authorId, playlistName);
        }
    }

    /**
     * 저장된 플레이리스트 재생
     *
     * @param event 메시지 이벤트
     * @param name 플레이리스트 이름
     */
    public void playMusicList(MessageReceivedEvent event, String name) {
        if (event.getMember() == null || event.getMember().getVoiceState() == null
                || !event.getMember().getVoiceState().inAudioChannel()) {
            return;
        }

        if (!event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
            final AudioManager audioManager = event.getGuild().getAudioManager();
            final VoiceChannel memberChannel = (VoiceChannel) event.getMember().getVoiceState().getChannel();
            audioManager.openAudioConnection(memberChannel);
        }

        TextChannel textChannel = event.getChannel().asTextChannel();

        if (name.isEmpty()) {
            List<MusicList> allMusic = musicListService.findAll();
            if (allMusic.isEmpty()) {
                return;
            }
            stateManager.setPendingSavedPlaylistRandomChoice(event.getAuthor().getId(), "");
        } else {
            List<MusicList> musicLists = musicListService.findByName(name);
            if (musicLists.isEmpty()) {
                return;
            }
            stateManager.setPendingSavedPlaylistRandomChoice(event.getAuthor().getId(), name);
        }
    }

    /**
     * 저장된 플레이리스트 재생 (셔플 옵션 포함)
     *
     * @param event 메시지 이벤트
     * @param name 플레이리스트 이름
     * @param shuffle 셔플 여부
     */
    private void playSavedMusicListWithShuffle(MessageReceivedEvent event, String name, boolean shuffle) {
        TextChannel textChannel = event.getChannel().asTextChannel();
        List<MusicList> musicLists;

        if (name.isEmpty()) {
            List<MusicList> allMusic = musicListService.findAll();
            if (allMusic.isEmpty()) {
                return;
            }

            if (shuffle) {
                java.util.Collections.shuffle(allMusic);
                int selectCount = Math.min(10, allMusic.size());
                musicLists = allMusic.subList(0, selectCount);
            } else {
                int selectCount = Math.min(10, allMusic.size());
                musicLists = allMusic.subList(0, selectCount);
            }
        } else {
            musicLists = musicListService.findByName(name);
            if (musicLists.isEmpty()) {
                return;
            }

            if (shuffle) {
                java.util.Collections.shuffle(musicLists);
            }
        }

        for (MusicList music : musicLists) {
            PlayerManager.getINSTANCE().loadAndPlay(textChannel, music.getUrl(), event.getMember());
        }
    }

    /**
     * 저장된 플레이리스트 목록 표시
     *
     * @param event 메시지 이벤트
     */
    public void showSavedMusicList(MessageReceivedEvent event) {
        TextChannel textChannel = event.getChannel().asTextChannel();
        List<MusicList> musicLists = musicListService.findAll();

        if (musicLists.isEmpty()) {
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🎵 저장된 음악 목록");
        embedBuilder.setColor(Color.CYAN);
        embedBuilder.setDescription("총 " + musicLists.size() + "개의 곡이 저장되어 있습니다.");
        embedBuilder.setFooter("`!pl <이름>`으로 재생할 수 있습니다.");
    }
}