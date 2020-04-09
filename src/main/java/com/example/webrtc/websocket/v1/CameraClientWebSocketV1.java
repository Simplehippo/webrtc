package com.example.webrtc.websocket.v1;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@ServerEndpoint("/v1/client/camera")
@Component
public class CameraClientWebSocketV1 {

    private static final BlockingQueue<String> workQueue = new LinkedBlockingQueue<>();
    private static final AtomicInteger cameraClientNum = new AtomicInteger(0);
    private static final ConcurrentMap<String, CameraClientWebSocketV1> webSocketMap = new ConcurrentHashMap<>();
    private Session session;
    private String identity;

    static {
        Executors.newSingleThreadExecutor().execute(() -> {
            while(true) {
                try {
                    CameraServerWebSocketV1.sendMessage(workQueue.take());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.identity = UUID.randomUUID().toString();
        webSocketMap.put(identity, this);
        int curNum = cameraClientNum.incrementAndGet();
        log.info("camera client num: " + curNum);
    }

    @OnClose
    public void onClose() {
        webSocketMap.remove(identity);
        int curNum = cameraClientNum.decrementAndGet();
        log.info("camera client num: " + curNum);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        if (StringUtils.isNotBlank(message)) {
            JSONObject jsonObject = JSON.parseObject(message);
            jsonObject.put("identity", this.identity);
            workQueue.add(jsonObject.toJSONString());
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("camera client error:" + this.identity + ", Cause: " + error.getMessage());
        error.printStackTrace();
    }
}