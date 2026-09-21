package com.gameexpert.ws.handler;

import com.gameexpert.api.SessionRegistry;
import com.gameexpert.ws.NicknameHandshakeInterceptor;
import com.gameexpert.ws.WorldBroadcaster;
import com.gameexpert.ws.WorldSessionRegistry;
import com.gameexpert.ws.WsMessageContext;
import com.gameexpert.ws.dto.OnlineUsersResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OnlineUsersWsHandler implements WsMessageHandler {
    private final WorldSessionRegistry registry;
    private final WorldBroadcaster broadcaster;

    @Override
    public String type() {
        return "onlineUsers";
    }

    @Override
    public void handle(WsMessageContext context, JsonNode message) {
        // TODO Lv 15: 현재 월드의 열린 연결에서 닉네임을 조회하고 요청자에게 응답합니다.

        Collection<SessionRegistry.Entry> entries = registry.entries(context.worldId());

        List<String> users = new ArrayList<>();

        for(SessionRegistry.Entry b : entries){
            if(b.session().isOpen()){
                users.add(b.session().getAttributes().get(NicknameHandshakeInterceptor.ATTR_NICKNAME).toString());
            }
        }
        Collections.sort(users);

//        List<String> users = entries.stream()
//                .map(SessionRegistry.Entry::session)              // Entry -> WebSocketSession
//                .filter(WebSocketSession::isOpen)                  // 열려있는 세션만
//                .map(session -> (String) session.getAttributes()
//                        .get(NicknameHandshakeInterceptor.ATTR_NICKNAME))  // 세션 -> 닉네임
//                .sorted()                                            // 알파벳순 정렬
//                .toList(); // AI 답변

        broadcaster.sendTo(context.session(), new OnlineUsersResponse(users,users.size()));

    }
}
