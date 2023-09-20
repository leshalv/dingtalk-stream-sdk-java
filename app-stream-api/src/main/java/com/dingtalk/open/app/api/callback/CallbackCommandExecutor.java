package com.dingtalk.open.app.api.callback;

import com.dingtalk.open.app.api.DingTalkAppError;
import com.dingtalk.open.app.api.OpenDingTalkAppException;
import com.dingtalk.open.app.api.protocol.MessageConverter;
import com.dingtalk.open.app.api.protocol.CommandExecutor;
import com.dingtalk.open.app.api.protocol.MessageConverterMapping;
import com.dingtalk.open.app.stream.network.api.Context;
import com.dingtalk.open.app.stream.network.api.logger.InternalLogger;
import com.dingtalk.open.app.stream.network.api.logger.InternalLoggerFactory;
import com.dingtalk.open.app.stream.protocol.ContentType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author feiyin
 * @date 2023/3/17
 */
public class CallbackCommandExecutor implements CommandExecutor {

    private static final InternalLogger LOGGER = InternalLoggerFactory.getLogger(CallbackCommandExecutor.class);

    public final Map<String, CallbackDescriptor> callbackDescriptors;

    public CallbackCommandExecutor() {
        this.callbackDescriptors = new HashMap<>();
    }

    public void register(String service, OpenDingTalkCallbackListener<?, ?> callback) {
        callbackDescriptors.put(service, CallbackDescriptor.build(callback));
    }

    @Override
    public void execute(Context context) {
        CallbackDescriptor descriptor = callbackDescriptors.get(context.getRequest().getTopic());
        if (descriptor == null) {
            context.exception(new OpenDingTalkAppException(DingTalkAppError.TOPIC_NOT_EXIST));
            return;
        }

        MessageConverter converter = MessageConverterMapping.getConverter(ContentType.of(context.getRequest().getContentType()));
        if (converter == null) {
            context.exception(DingTalkAppError.UNKNOWN_CONTENT_TYPE.toException());
            return;
        }
        Object parameter = converter.convert(context.getRequest().getData(), descriptor.getParameterType());
        Object payload;
        try {
            payload = descriptor.getMethod().execute(parameter);
        } catch (Exception e) {
            LOGGER.error("[DingTalk] execute callback failed, topic={}", context.getRequest().getTopic(), e);
            context.exception(e);
            return;
        }
        context.replay(payload);
    }
}
