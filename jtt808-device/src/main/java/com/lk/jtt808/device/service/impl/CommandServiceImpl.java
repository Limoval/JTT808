package com.lk.jtt808.device.service.impl;


import com.lk.jtt808.protocol.entity.T0107;
import com.lk.jtt808.protocol.entity.T8107;
import com.lk.jtt808.device.session.Session;
import com.lk.jtt808.device.session.SessionManager;
import com.lk.jtt808.device.service.CommandService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class CommandServiceImpl implements CommandService {

    @Autowired
    private SessionManager sessionManager;

    @Override
    public T0107 getClientParameter(String clientId) {
        Session session = sessionManager.getSession(clientId);
        if (session == null || !session.isRegistered()) {
            return null;
        }
        T8107 t8107 = new T8107();
        t8107.setClientId(clientId);
        return session.sendRequest(t8107, T0107.class).timeout(Duration.ofSeconds(10)).block();
    }




}
