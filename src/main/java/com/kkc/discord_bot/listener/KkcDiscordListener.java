// 생성됨 - 2025-10-14
package com.kkc.discord_bot.listener;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Discord 봇의 기본 이벤트 리스너
 * 간단한 명령어 처리 및 UI 인터랙션을 담당합니다.
 * 
 * 주요 기능:
 * - 기본 메시지 명령어 처리 (!딜리버리야, !재생)
 * - 버튼 인터랙션 처리
 * - 슬래시 명령어 처리
 * - 모달(입력창) 인터랙션 처리
 *
 * @author KKC
 * @since 2025-10-14
 */
@Slf4j
public class KkcDiscordListener extends ListenerAdapter {
    
    // ========== 상수 정의 ==========
    
    /** 명령어 접두사 */
    private static final String COMMAND_PREFIX = "!딜리버리야";
    
    /** 재생 명령어 */
    private static final String PLAY_COMMAND = "!재생";
    
    /** 인삿말 응답 메시지 포맷 */
    private static final String GREETING_FORMAT = "%s님 안녕하세요~";
    
    /** 알 수 없는 명령어 응답 */
    private static final String UNKNOWN_COMMAND_MSG = "이 쉐끼는 없는걸 쳐넣네";
    
    /** 버튼 ID */
    private static final String PLAY_BUTTON_ID = "play-button";
    private static final String PAUSE_BUTTON_ID = "pause-button";
    private static final String STOP_BUTTON_ID = "stop-button";
    private static final String HELLO_BUTTON_ID = "hello-button";
    private static final String INPUT_BUTTON_ID = "input-button";
    
    /** 모달 ID */
    private static final String GREETING_MODAL_ID = "greeting-modal";
    private static final String GREETING_INPUT_ID = "greeting-input";
    
    // ========== 상태 관리 ==========
    
    /** 인삿말 입력 모드를 추적하기 위한 Set (사용자 ID 저장) */
    private final Set<String> waitingForGreeting = new HashSet<>();

    /**
     * 메시지 수신 이벤트 처리
     * 사용자가 보낸 메시지를 분석하고 적절한 응답을 생성합니다.
     *
     * @param event 메시지 수신 이벤트
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // 기본 정보 추출
        User user = event.getAuthor();
        TextChannel textChannel = event.getChannel().asTextChannel();
        Message message = event.getMessage();
        
        log.debug("메시지 수신 - 사용자: {}, 내용: {}", user.getName(), message.getContentDisplay());

        // 봇이 보낸 메시지는 무시
        if (user.isBot()) {
            return;
        }
        
        // 빈 메시지 체크
        if (message.getContentDisplay().isEmpty()) {
            log.warn("빈 메시지 수신 - 사용자: {}", user.getName());
            return;
        }

        // 인삿말 입력 대기 중인 경우 처리
        if (handleGreetingInput(user, textChannel, message)) {
            return;
        }

        // 메시지를 공백으로 분리하여 명령어 파싱
        String[] messageArray = message.getContentDisplay().split(" ");
        String command = messageArray[0];
        
        // 명령어별 처리
        if (command.equalsIgnoreCase(COMMAND_PREFIX)) {
            handleDeliveryCommand(event, messageArray, textChannel);
        } else if (command.equalsIgnoreCase(PLAY_COMMAND)) {
            showMusicControlButtons(textChannel);
        }
    }

    /**
     * 인삿말 입력 대기 상태 처리
     *
     * @param user 사용자 정보
     * @param textChannel 텍스트 채널
     * @param message 메시지
     * @return 인삿말 입력이 처리되었으면 true, 아니면 false
     */
    private boolean handleGreetingInput(User user, TextChannel textChannel, Message message) {
        if (!waitingForGreeting.contains(user.getId())) {
            return false;
        }
        
        String greeting = message.getContentDisplay();
        
        // 인삿말 임베드 메시지 생성 및 전송
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("인사")
                .setDescription(greeting)
                .setColor(Color.GREEN)
                .setFooter(user.getName() + "님의 인사", user.getAvatarUrl());
        
        // textChannel.sendMessageEmbeds(embed.build()).queue();
        
        // 입력 모드 종료
        waitingForGreeting.remove(user.getId());
        log.info("인삿말 처리 완료 - 사용자: {}", user.getName());
        
        return true;
    }

    /**
     * !딜리버리야 명령어 처리
     *
     * @param event 메시지 이벤트
     * @param messageArray 분리된 메시지 배열
     * @param textChannel 텍스트 채널
     */
    private void handleDeliveryCommand(MessageReceivedEvent event, String[] messageArray, TextChannel textChannel) {
        // 첫 번째 요소(명령어)를 제외한 나머지 인자 추출
        String[] messageArgs = Arrays.copyOfRange(messageArray, 1, messageArray.length);
        
        log.info("딜리버리 명령어 실행 - 인자 개수: {}", messageArgs.length);
        
        for (String msg : messageArgs) {
            String response = processMessage(event, msg);
            // textChannel.sendMessage(response).queue();
        }
    }

    /**
     * 개별 메시지 처리 (명령어 인자)
     *
     * @param event 메시지 이벤트
     * @param message 처리할 메시지
     * @return 응답 메시지
     */
    private String processMessage(MessageReceivedEvent event, String message) {
        log.debug("메시지 처리: {}", message);
        
        User user = event.getAuthor();
        
        switch (message) {
            case "안녕":
                log.info("인사 명령어 실행 - 사용자: {}", user.getName());
                return String.format(GREETING_FORMAT, user.getName());
            default:
                log.warn("알 수 없는 명령어: {}", message);
                return UNKNOWN_COMMAND_MSG;
        }
    }

    /**
     * 음악 컨트롤 버튼 UI 표시
     *
     * @param textChannel 텍스트 채널
     */
    private void showMusicControlButtons(TextChannel textChannel) {
        // 임베드 메시지 생성
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("음악 재생")
                .setDescription("원하시는 버튼을 선택하세요!")
                .setColor(Color.GREEN);

        // 버튼 생성
        Button playButton = Button.success(PLAY_BUTTON_ID, "재생");
        Button pauseButton = Button.primary(PAUSE_BUTTON_ID, "일시정지");
        Button stopButton = Button.danger(STOP_BUTTON_ID, "정지");

        // 임베드와 버튼을 포함한 메시지 전송
        // textChannel.sendMessageEmbeds(embed.build())
        //         .setActionRow(playButton, pauseButton, stopButton)
        //         .queue();
        
        log.info("음악 컨트롤 버튼 표시");
    }

    /**
     * 버튼 클릭 이벤트 처리
     *
     * @param event 버튼 인터랙션 이벤트
     */
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        
        log.info("버튼 클릭 - ID: {}, 사용자: {}", buttonId, event.getUser().getName());
        
        switch (buttonId) {
            case PLAY_BUTTON_ID:
                event.reply("재생을 시작합니다!").queue();
                break;
            case PAUSE_BUTTON_ID:
                event.reply("일시정지되었습니다.").queue();
                break;
            case STOP_BUTTON_ID:
                event.reply("재생을 정지합니다.").queue();
                break;
            case HELLO_BUTTON_ID:
                event.reply("안녕하세요!").queue();
                break;
            case INPUT_BUTTON_ID:
                showGreetingModal(event);
                break;
            default:
                log.warn("알 수 없는 버튼 ID: {}", buttonId);
        }
    }

    /**
     * 인삿말 입력 모달 표시
     *
     * @param event 버튼 인터랙션 이벤트
     */
    private void showGreetingModal(ButtonInteractionEvent event) {
        // TextInput 생성
        TextInput greetingInput = TextInput.create(GREETING_INPUT_ID, "인삿말", TextInputStyle.SHORT)
                .setPlaceholder("인삿말을 입력해주세요")
                .setMinLength(1)
                .setMaxLength(100)
                .setRequired(true)
                .build();

        // Modal 생성
        Modal modal = Modal.create(GREETING_MODAL_ID, "인사하기")
                .addActionRow(greetingInput)
                .build();

        // Modal 표시
        event.replyModal(modal).queue();
        log.info("인삿말 모달 표시 - 사용자: {}", event.getUser().getName());
    }

    /**
     * 슬래시 명령어 처리
     *
     * @param event 슬래시 명령어 이벤트
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        
        log.info("슬래시 명령어 실행 - 명령어: {}, 사용자: {}", commandName, event.getUser().getName());
        
        if ("greet".equals(commandName)) {
            handleGreetSlashCommand(event);
        } else if ("hello".equals(commandName)) {
            handleHelloSlashCommand(event);
        }
    }

    /**
     * /greet 슬래시 명령어 처리
     *
     * @param event 슬래시 명령어 이벤트
     */
    private void handleGreetSlashCommand(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("인사하기")
                .setDescription("아래 버튼을 눌러보세요!")
                .setColor(Color.BLUE);

        Button button = Button.primary(HELLO_BUTTON_ID, "Hello");
        Button inputButton = Button.success(INPUT_BUTTON_ID, "인삿말 입력하기");

        event.replyEmbeds(embed.build())
                .addActionRow(button, inputButton)
                .queue();
    }

    /**
     * /hello 슬래시 명령어 처리
     * 사용자를 인삿말 입력 대기 상태로 전환합니다.
     *
     * @param event 슬래시 명령어 이벤트
     */
    private void handleHelloSlashCommand(SlashCommandInteractionEvent event) {
        // 사용자 ID를 인삿말 대기 목록에 추가
        waitingForGreeting.add(event.getUser().getId());
        
        // 안내 메시지 전송
        event.reply("인삿말을 입력해주세요!").queue();
        
        log.info("인삿말 입력 대기 상태 활성화 - 사용자: {}", event.getUser().getName());
    }

    /**
     * 모달 제출 이벤트 처리
     *
     * @param event 모달 인터랙션 이벤트
     */
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!GREETING_MODAL_ID.equals(event.getModalId())) {
            return;
        }
        
        String greeting = event.getValue(GREETING_INPUT_ID).getAsString();
        
        log.info("모달 제출 - 인삿말: {}, 사용자: {}", greeting, event.getUser().getName());

        // Embed 생성
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("인사")
                .setDescription(greeting)
                .setColor(Color.GREEN)
                .setFooter(event.getUser().getName() + "님의 인사", event.getUser().getAvatarUrl());

        // 응답 전송
        event.replyEmbeds(embed.build()).queue();
    }

}
