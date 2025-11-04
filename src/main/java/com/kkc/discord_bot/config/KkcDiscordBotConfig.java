// 생성됨 - 2025-10-14
package com.kkc.discord_bot.config;

import com.kkc.discord_bot.listener.ChattingReaction;
import com.kkc.discord_bot.listener.KkcDiscordListener;
import com.kkc.discord_bot.listener.MusicGuiListener;
import com.kkc.discord_bot.listener.VoiceChannelListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.security.auth.login.LoginException;

/**
 * Discord 봇 설정 클래스
 * JDA(Java Discord API) 인스턴스를 생성하고 필요한 이벤트 리스너를 등록합니다.
 *
 * @author KKC
 * @since 2025-10-14
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KkcDiscordBotConfig {

    @Value("${discord_token}")
    private String token;

    private final ChattingReaction chattingReaction;
    private final com.kkc.discord_bot.service.MusicListService musicListService;

    /**
     * JDA 인스턴스를 생성하고 설정합니다.
     * - Discord 봇 토큰으로 인증
     * - 필요한 게이트웨이 인텐트 활성화 (메시지, 음성 채널)
     * - 이벤트 리스너 등록
     * - 봇 활동 상태 설정
     *
     * @return 설정된 JDA 인스턴스
     * @throws LoginException       로그인 실패 시
     * @throws InterruptedException 봇 준비 대기 중 인터럽트 발생 시
     */
    @Bean
    public JDA jda() throws LoginException, InterruptedException {
        log.info("Discord 봇 초기화 시작...");

        JDA jda = JDABuilder.createDefault(token)
                            .enableIntents(
                                    GatewayIntent.MESSAGE_CONTENT,      // 메시지 내용 읽기
                                    GatewayIntent.GUILD_MESSAGES,       // 서버 메시지 수신
                                    GatewayIntent.GUILD_VOICE_STATES    // 음성 채널 상태 감지
                            )
                            //대기메시지 상태창
                            .setActivity(Activity.playing("메세지 대기"))
                            .addEventListeners(
                                    new KkcDiscordListener(),           // 기본 명령어 리스너
                                    chattingReaction,                   // 음악 재생 리스너 (Spring Bean 주입)
                                    new VoiceChannelListener(),         // 음성 채널 이벤트 리스너
                                    new MusicGuiListener(musicListService) // 음악 GUI 리스너 (MusicListService 주입)
                            )
                            .build();

        // 봇이 완전히 준비될 때까지 대기
        jda.awaitReady();

        log.info("Discord 봇 초기화 완료!");
        return jda;
    }

}