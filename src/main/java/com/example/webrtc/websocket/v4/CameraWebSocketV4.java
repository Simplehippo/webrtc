package com.example.webrtc.websocket.v4;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author simplehippo
 * @version 1.0
 */
@Slf4j
@Component
@ServerEndpoint("/v4/monitor")
public class CameraWebSocketV4 {

    private static final AtomicInteger cameraNum = new AtomicInteger(0);
    private static final AtomicInteger serverNum = new AtomicInteger(0);
    private static final ConcurrentMap<String, CameraWebSocketV4> cameras = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, CameraWebSocketV4> servers = new ConcurrentHashMap<>();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Session session;
    private String name;
    private boolean isServer;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.name = UUID.randomUUID().toString();
        this.isServer = false;
    }

    @OnClose
    public void onClose() {
        int cNum = cameraNum.get();
        int sNum = serverNum.get();
        if (isServer) {
            servers.remove(name);
            sNum = serverNum.decrementAndGet();
        } else {
            cameras.remove(name);
            cNum = cameraNum.decrementAndGet();
        }

        JSONObject response = new JSONObject();
        response.put("type", "leave");
        response.put("name", this.name);
        if (this.isServer) {
            cameras.forEach((k, instance) -> {
                sendMessage(instance.session, response.toJSONString());
            });
        } else {
            servers.forEach((k, instance) -> {
                sendMessage(instance.session, response.toJSONString());
            });
        }

        log.info(String.format("camera num: %d, server num: %d", cNum, sNum));
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            log.info("camera message: " + message);
            JSONObject data = JSON.parseObject(message);
            JSONObject response = new JSONObject();
            switch (data.getString("type")) {
                case "login":
                    Boolean isServer = data.getBoolean("isServer");
                    String name = data.getString("name");
                    if (cameras.containsKey(name) || servers.containsKey(name)) {
                        response.put("type", "login");
                        response.put("success", false);
                    } else if (isServer != null && isServer) {
                        this.name = name;
                        this.isServer = true;
                        servers.put(name, this);
                        serverNum.incrementAndGet();
                        response.put("type", "login");
                        response.put("success", true);
                    } else {
                        this.name = name;
                        cameras.put(name, this);
                        cameraNum.incrementAndGet();
                        response.put("type", "login");
                        response.put("success", true);
                    }

                    sendMessage(this.session, response.toJSONString());
                    log.info(String.format("camera num: %d, server num: %d", cameraNum.get(), serverNum.get()));
                    break;
                case "broadcast":
                    if (this.isServer) {
                        cameras.forEach((k, instance) -> {
                            JSONObject broadcast = new JSONObject();
                            broadcast.put("type", "connect");
                            broadcast.put("name", this.name);
                            sendMessage(instance.session, broadcast.toJSONString());
                        });
                    } else {
                        servers.forEach((k, instance) -> {
                            JSONObject broadcast = new JSONObject();
                            broadcast.put("type", "connect");
                            broadcast.put("name", instance.name);
                            sendMessage(this.session, broadcast.toJSONString());
                        });
                    }
                    break;
                case "offer":
                    String toOfferName = data.getString("name");
                    response.put("type", "offer");
                    response.put("offer", data.get("offer"));
                    response.put("name", this.name);
                    if (servers.containsKey(toOfferName)) {
                        sendMessage(servers.get(toOfferName).session, response.toJSONString());
                    }
                    break;
                case "answer":
                    String toAnswerName = data.getString("name");
                    response.put("type", "answer");
                    response.put("answer", data.get("answer"));
                    response.put("name", this.name);
                    if (cameras.containsKey(toAnswerName)) {
                        sendMessage(cameras.get(toAnswerName).session, response.toJSONString());
                    }
                    break;
                case "candidate":
                    String toCandidateName = data.getString("name");
                    response.put("type", "candidate");
                    response.put("candidate", data.get("candidate"));
                    response.put("name", this.name);
                    if (cameras.containsKey(toCandidateName)) {
                        sendMessage(cameras.get(toCandidateName).session, response.toJSONString());
                    } else if (servers.containsKey(toCandidateName)) {
                        sendMessage(servers.get(toCandidateName).session, response.toJSONString());
                    }
                    break;
                case "leave":
                    response.put("type", "leave");
                    response.put("name", this.name);
                    if (this.isServer) {
                        cameras.forEach((k, instance) -> {
                            sendMessage(instance.session, response.toJSONString());
                        });
                    } else {
                        servers.forEach((k, instance) -> {
                            sendMessage(instance.session, response.toJSONString());
                        });
                    }
                    break;
                default:
                    String unknowType = data.getString("type");
                    response.put("type", "error");
                    response.put("message", "Command not found: " + unknowType);
                    this.session.getBasicRemote().sendText(response.toJSONString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("error:" + this.name + ", Cause: " + error.getMessage());
        error.printStackTrace();
    }


    private static void sendMessage(Session session, String message) {
        executorService.execute(new WorkTask(session, message));
    }

    private static class WorkTask implements Runnable {
        private Session session;
        private String message;

        private WorkTask(Session session, String message) {
            this.session = session;
            this.message = message;
        }


        @Override
        public void run() {
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
