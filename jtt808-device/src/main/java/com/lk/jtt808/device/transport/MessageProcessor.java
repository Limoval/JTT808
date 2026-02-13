package com.lk.jtt808.device.transport;

import com.lk.jtt808.device.handler.MessageHandlerDispatcher;
import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.protocol.entity.JT808Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 传输无关的消息处理器
 * 从 JTT808ServerHandler 提取，处理响应匹配和消息分发
 */
@Component
@Slf4j
public class MessageProcessor {

    private final MessageHandlerDispatcher dispatcher;

    public MessageProcessor(MessageHandlerDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * 处理收到的消息
     *
     * @param message 协议消息
     * @param session 传输会话
     */
    public void process(JT808Message message, TransportSession session) {
        // 更新最后访问时间
        session.updateLastAccessTime();

        // 尝试匹配等待中的请求-响应
        if (message instanceof JT808Response response) {
            if (session.handleResponse(response)) {
                log.debug("响应已匹配: messageId=0x{}, clientId={}",
                        Integer.toHexString(message.getMessageId()),
                        message.getClientId());
                return;
            }
        }

        // 分发到对应的 Handler
        try {
            dispatcher.dispatch(message, session);
        } catch (UnsupportedOperationException e) {
            log.warn("未找到处理器: messageId=0x{}, clientId={}",
                    Integer.toHexString(message.getMessageId()),
                    message.getClientId());
        } catch (Exception e) {
            log.error("消息处理异常: messageId=0x{}, clientId={}",
                    Integer.toHexString(message.getMessageId()),
                    message.getClientId(), e);
        }
    }
}
