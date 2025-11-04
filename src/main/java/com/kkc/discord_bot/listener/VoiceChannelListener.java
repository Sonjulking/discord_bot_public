// 생성 시간: 2025-10-13
// 생성됨
package com.kkc.discord_bot.listener;

import com.kkc.discord_bot.music.GuildMusicManager;
import com.kkc.discord_bot.music.PlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class VoiceChannelListener extends ListenerAdapter {

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();

        // 봇 자신의 이벤트는 무시
        if (member.getUser().isBot()) {
            return;
        }

        // 봇이 연결된 음성 채널 확인
        VoiceChannel botChannel = (VoiceChannel) guild.getAudioManager().getConnectedChannel();
        if (botChannel == null) {
            return;
        }

        // 사용자가 봇과 같은 채널에서 나갔거나 들어왔는지 확인
        VoiceChannel leftChannel = (VoiceChannel) event.getChannelLeft();
        VoiceChannel joinedChannel = (VoiceChannel) event.getChannelJoined();

        boolean affectsBotChannel = (leftChannel != null && leftChannel.equals(botChannel)) ||
                (joinedChannel != null && joinedChannel.equals(botChannel));

        if (affectsBotChannel) {
            // PlayerManager를 통해 TrackScheduler에 접근하여 체크
            GuildMusicManager musicManager = PlayerManager.getINSTANCE().getMusicManager(guild);
            if (musicManager != null) {
                musicManager.scheduler.checkAndStartAloneTimer();
            }
        }
    }
}
