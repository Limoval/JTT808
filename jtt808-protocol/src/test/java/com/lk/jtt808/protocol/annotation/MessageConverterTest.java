package com.lk.jtt808.protocol.annotation;

import com.lk.jtt808.protocol.entity.T0200;
import com.lk.jtt808.protocol.entity.T0800;
import com.lk.jtt808.protocol.entity.T8004;
import com.lk.jtt808.protocol.entity.T8106;
import com.lk.jtt808.protocol.entity.T8300;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageConverterTest {

    @Test
    void shouldEncodeAndParseFixedBcdField() throws Exception {
        T8004 message = new T8004();
        message.setServerTime("260507123456");

        ByteBuf buf = Unpooled.buffer();
        try {
            MessageConverter.toByteBuf(buf, message);

            assertThat(ByteBufUtil.hexDump(buf)).isEqualTo("260507123456");
            ByteBuf copy = buf.copy();
            try {
                T8004 parsed = MessageConverter.parse(copy, T8004.class);
                assertThat(parsed.getServerTime()).isEqualTo("260507123456");
            } finally {
                copy.release();
            }
        } finally {
            buf.release();
        }
    }

    @Test
    void shouldSupportUnsignedDwordRange() throws Exception {
        T0800 message = new T0800();
        message.setMediaId(0xFFFF_FFFFL);
        message.setMediaType(1);
        message.setFormatCode(0);
        message.setEventCode(2);
        message.setChannelId(3);

        ByteBuf buf = Unpooled.buffer();
        try {
            MessageConverter.toByteBuf(buf, message);

            ByteBuf copy = buf.copy();
            try {
                T0800 parsed = MessageConverter.parse(copy, T0800.class);
                assertThat(parsed.getMediaId()).isEqualTo(0xFFFF_FFFFL);
                assertThat(parsed.getMediaType()).isEqualTo(1);
                assertThat(parsed.getEventCode()).isEqualTo(2);
            } finally {
                copy.release();
            }
        } finally {
            buf.release();
        }
    }

    @Test
    void shouldRejectNullRequiredField() {
        T8300 message = new T8300();
        message.setFlag(1);

        ByteBuf buf = Unpooled.buffer();
        try {
            assertThatThrownBy(() -> MessageConverter.toByteBuf(buf, message))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Required field cannot be null");
        } finally {
            buf.release();
        }
    }

    @Test
    void shouldRejectTruncatedBody() {
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[] {0x00, 0x00, 0x00, 0x01});
        try {
            assertThatThrownBy(() -> MessageConverter.parse(buf, T0800.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient readable bytes");
        } finally {
            buf.release();
        }
    }

    @Test
    void shouldRoundTripCustomParameterIdList() throws Exception {
        T8106 message = new T8106();
        message.setParameterIds(List.of(1L, 0xFFFF_FFFFL));

        ByteBuf buf = Unpooled.buffer();
        try {
            MessageConverter.toByteBuf(buf, message);

            ByteBuf copy = buf.copy();
            try {
                T8106 parsed = MessageConverter.parse(copy, T8106.class);
                assertThat(parsed.getParameterCount()).isEqualTo(2);
                assertThat(parsed.getParameterIds()).containsExactly(1L, 0xFFFF_FFFFL);
            } finally {
                copy.release();
            }
        } finally {
            buf.release();
        }
    }

    @Test
    void shouldKeepLocationSpeedAsProtocolRawValue() throws Exception {
        T0200 message = new T0200();
        message.setAlarmFlag(0L);
        message.setStatusFlag(0L);
        message.setLatitude(31.123456);
        message.setLongitude(121.123456);
        message.setAltitude(12);
        message.setSpeed(345);
        message.setDirection(90);
        message.setTime("260507123456");
        message.setAdditionalInfos(Collections.emptyList());

        ByteBuf buf = Unpooled.buffer();
        try {
            MessageConverter.toByteBuf(buf, message);

            ByteBuf copy = buf.copy();
            try {
                T0200 parsed = MessageConverter.parse(copy, T0200.class);
                assertThat(parsed.getSpeed()).isEqualTo(345);
                assertThat(parsed.getTime()).isEqualTo("260507123456");
                assertThat(parsed.getAdditionalInfos()).isEmpty();
            } finally {
                copy.release();
            }
        } finally {
            buf.release();
        }
    }
}
