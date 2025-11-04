package com.kkc.discord_bot.command;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class CommandRegistrar {
    private final JDA jda;

    public CommandRegistrar(JDA jda) {
        this.jda = jda;
        registerCommands();
    }

    private void registerCommands() {
        // 글로벌 커맨드 등록
        jda.updateCommands().addCommands(
                Commands.slash("greet", "인사 메시지와 버튼을 표시합니다."),
                Commands.slash("hello", "인삿말을 입력합니다")
                        .addOption(OptionType.STRING, "message", "전하고 싶은 인삿말을 입력하세요", true)
        ).queue();

    }
}
