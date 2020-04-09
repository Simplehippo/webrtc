package com.example.webrtc.websocket.v2;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author simplehippo
 * @version 1.0
 */
@Slf4j
@Component
@ServerEndpoint("/v2/camera")
public class CameraWebSocketV2 {

    private static final AtomicInteger cameraNum = new AtomicInteger(0);
    private static final ConcurrentMap<String, CameraWebSocketV2> users = new ConcurrentHashMap<>();
    private Session session;
    private String name;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.name = UUID.randomUUID().toString();
        log.info("camera num: " + cameraNum.incrementAndGet());
    }

    @OnClose
    public void onClose() {
        users.remove(name);
        log.info("camera num: " + cameraNum.decrementAndGet());
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            log.info("camera message: " + message);
            JSONObject data = JSON.parseObject(message);
            JSONObject response = new JSONObject();
            switch (data.getString("type")) {
                case "login":
                    String name = data.getString("name");
                    if (users.containsKey(name)) {
                        response.put("type", "login");
                        response.put("success", false);
                    } else {
                        this.name = name;
                        users.put(name, this);
                        response.put("type", "login");
                        response.put("success", true);
                    }

                    this.session.getBasicRemote().sendText(response.toJSONString());
                    break;
                case "offer":
                    String toOfferName = data.getString("name");
                    response.put("type", "offer");
                    response.put("offer", data.get("offer"));
                    response.put("name", this.name);
                    if (users.containsKey(toOfferName)) {
                        users.get(toOfferName).session.getBasicRemote().sendText(response.toJSONString());
                    }
                    break;
                case "answer":
                    String toAnswerName = data.getString("name");
                    response.put("type", "answer");
                    response.put("answer", data.get("answer"));
                    response.put("name", this.name);
                    if (users.containsKey(toAnswerName)) {
                        users.get(toAnswerName).session.getBasicRemote().sendText(response.toJSONString());
                    }
                    break;
                case "candidate":
                    String toCandidateName = data.getString("name");
                    response.put("type", "candidate");
                    response.put("candidate", data.get("candidate"));
                    response.put("name", this.name);
                    if (users.containsKey(toCandidateName)) {
                        users.get(toCandidateName).session.getBasicRemote().sendText(response.toJSONString());
                    }
                    break;
                case "leave":
                    String toLeaveName = data.getString("name");
                    response.put("type", "leave");
                    this.session.getBasicRemote().sendText(response.toJSONString());
                    if (toLeaveName != null && users.containsKey(toLeaveName)) {
                        users.get(toLeaveName).session.getBasicRemote().sendText(response.toJSONString());
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
        log.error("camera error:" + this.name + ", Cause: " + error.getMessage());
        error.printStackTrace();
    }
}
