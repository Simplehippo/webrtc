package com.example.webrtc.websocket.v1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@ServerEndpoint("/v1/camera/server")
@Component
public class CameraServerWebSocketV1 {

    private static final AtomicInteger cameraServerNum = new AtomicInteger(0);
    private static final ConcurrentMap<String, CameraServerWebSocketV1> webSocketMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, BlockingQueue<String>> messageQueueMap = new ConcurrentHashMap<>();
    private static final ExecutorService executorService = Executors.newFixedThreadPool(100);
    private Session session;
    private String token;
    private BlockingQueue<String> workQueue;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.token = UUID.randomUUID().toString();
        this.workQueue = new LinkedBlockingQueue<>();
        executorService.execute(new WorkerTask(session, workQueue));
        webSocketMap.put(token, this);

        int curNum = cameraServerNum.incrementAndGet();
        log.info("camera server num: " + curNum);
    }

    @OnClose
    public void onClose() {
        webSocketMap.remove(token);

        int curNum = cameraServerNum.decrementAndGet();
        log.info("camera server num: " + curNum);
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("camera server message: " + message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("camera server error:" + this.token + ", Cause: " + error.getMessage());
        error.printStackTrace();
    }

    public static void sendMessage(String message) {
        if (!webSocketMap.isEmpty()) {
            webSocketMap.forEach((token, instance) -> instance.workQueue.add(message));
        }
    }

    private static class WorkerTask implements Runnable {

        private Session session;
        private BlockingQueue<String> workQueue;

        private WorkerTask(Session session, BlockingQueue<String> workQueue) {
            this.session = session;
            this.workQueue = workQueue;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    String message = workQueue.take();
                    if (session.isOpen()) {
                        session.getBasicRemote().sendText(message);
                    } else {
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}