// 생성됨 - 2025-11-04 자동 GUI 새로고침 기능 추가
package com.kkc.discord_bot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class GuildMusicManager {

    public final AudioPlayer audioPlayer;
    public final TrackScheduler scheduler;
    private final AudioPlayerSendHandler sendHandler;
    public final TextChannel textChannel; // 🔹 추가됨
    
    // 🔹 GUI 메시지 ID 저장 (자동 새로고침용)
    private String guiMessageId;

    public GuildMusicManager(AudioPlayerManager manager, Guild guild, TextChannel textChannel) {
        this.audioPlayer = manager.createPlayer();
        this.scheduler = new TrackScheduler(this.audioPlayer, guild, textChannel, this);
        this.audioPlayer.addListener(this.scheduler);
        this.sendHandler = new AudioPlayerSendHandler(this.audioPlayer);
        this.textChannel = textChannel; // 🔹 초기화
    }

    public AudioPlayerSendHandler getSendHandler() {
        return this.sendHandler;
    }
    
    // 🔹 GUI 메시지 ID getter/setter
    public String getGuiMessageId() {
        return guiMessageId;
    }
    
    public void setGuiMessageId(String guiMessageId) {
        this.guiMessageId = guiMessageId;
    }
}