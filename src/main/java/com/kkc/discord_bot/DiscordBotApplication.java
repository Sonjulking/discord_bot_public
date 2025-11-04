// 생성됨 - 2025-10-14
package com.kkc.discord_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Discord 봇 애플리케이션의 메인 클래스
 * Spring Boot 애플리케이션을 시작하고 Discord 봇을 초기화합니다.
 *
 * @author KKC
 * @since 2025-10-14
 */
@SpringBootApplication
public class DiscordBotApplication {

    /**
     * 애플리케이션 진입점
     * Spring Boot 컨텍스트를 초기화하고 Discord 봇을 시작합니다.
     *
     * @param args 명령줄 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(DiscordBotApplication.class, args);
    }

}
