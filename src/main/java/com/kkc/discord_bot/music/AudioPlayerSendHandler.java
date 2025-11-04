package com.kkc.discord_bot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.Buffer;
import java.nio.ByteBuffer;

//Discord로오디오 데이터를 전송하기 위한 인터페이스를 구현
@RequiredArgsConstructor
/*
public AudioPlayerSendHandler(AudioPlayer audioPlayer) {
    this.audioPlayer = audioPlayer;
    this.buffer = ByteBuffer.allocate(1024);
    this.frame = new MutableAudioFrame();
    this.frame.setBuffer(buffer);
}
*/


public class AudioPlayerSendHandler implements AudioSendHandler {
    private final AudioPlayer audioPlayer;
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private final MutableAudioFrame frame = new MutableAudioFrame();

    //인스턴스 초기화 블록(instance initializer block)
    //블록은 객체가 생성될 때 필드 초기화 후, 생성자 실행 전에 자동으로 실행됩니다.
    //즉, 생성자에 넣지 않고도 공통적으로 실행되어야 하는 초기화 코드를 작성할 때 유용합니다.
    {
        frame.setBuffer(buffer);
    }

    @Override
    public boolean canProvide() {
        return this.audioPlayer.provide(this.frame);
    }

    /*    @Override
        public ByteBuffer provide20MsAudio() {
            final Buffer buffer = ((Buffer) this.buffer).flip();
            return (ByteBuffer) buffer;
        }*/
    @Override
    public ByteBuffer provide20MsAudio() {
        final Buffer buffer = ((Buffer) this.buffer).flip();
        return (ByteBuffer) buffer;
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}
