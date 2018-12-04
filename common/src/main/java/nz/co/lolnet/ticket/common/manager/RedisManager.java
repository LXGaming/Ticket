/*
 * Copyright 2018 lolnet.co.nz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nz.co.lolnet.ticket.common.manager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.TicketData;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.common.util.Toolbox;
import org.apache.commons.lang3.StringUtils;

public class RedisManager {
    
    public static void process(String message) {
        JsonObject jsonObject = Toolbox.parseJson(message, JsonObject.class).orElse(null);
        if (jsonObject == null) {
            return;
        }
        
        String id = Toolbox.parseJson(jsonObject.get("id"), String.class).orElse(null);
        if (StringUtils.isBlank(id)) {
            Ticket.getInstance().getLogger().warn("Received redis message with an invalid id");
            return;
        }
        
        UserData user = Toolbox.parseJson(jsonObject.get("user"), UserData.class).orElse(null);
        if (user == null) {
            Ticket.getInstance().getLogger().warn("Received redis message with an invalid user");
            return;
        }
        
        if (id.equalsIgnoreCase("UserBan")) {
        
        } else if (id.equalsIgnoreCase("UserPardon")) {
        
        }
        
        TicketData ticket = Toolbox.parseJson(jsonObject.get("ticket"), TicketData.class).orElse(null);
        if (ticket == null) {
            Ticket.getInstance().getLogger().warn("Received redis message with an invalid ticket");
            return;
        }
        
        if (id.equalsIgnoreCase("TicketClose")) {
            onTicketClose(ticket, user);
        } else if (id.equalsIgnoreCase("TicketComment")) {
            onTicketComment(ticket, user);
        } else if (id.equalsIgnoreCase("TicketOpen")) {
            onTicketOpen(ticket, user);
        } else if (id.equalsIgnoreCase("TicketReopen")) {
            onTicketReopen(ticket, user);
        }
    }
    
    public static void onUserBan(UserData user) {
        DataManager.getCachedUser(user.getUniqueId()).ifPresent(cachedUser -> {
            cachedUser.setName(user.getName());
            cachedUser.setBanned(user.isBanned());
        });
    }
    
    public static void onUserPardon(UserData user) {
        DataManager.getCachedUser(user.getUniqueId()).ifPresent(cachedUser -> {
            cachedUser.setName(user.getName());
            cachedUser.setBanned(user.isBanned());
        });
    }
    
    public static void onTicketClose(TicketData ticket, UserData user) {
        //DataManager.getTicketCache().put(ticket.getId(), ticket);
    }
    
    public static void onTicketComment(TicketData ticket, UserData user) {
        //DataManager.getTicketCache().put(ticket.getId(), ticket);
    }
    
    public static void onTicketOpen(TicketData ticket, UserData user) {
        //DataManager.getTicketCache().put(ticket.getId(), ticket);
    }
    
    public static void onTicketReopen(TicketData ticket, UserData user) {
        //DataManager.getTicketCache().put(ticket.getId(), ticket);
    }
    
    public static void sendMessage(String id, TicketData ticket, UserData user) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", id);
        jsonObject.add("ticket", new Gson().toJsonTree(ticket));
        jsonObject.add("user", new Gson().toJsonTree(user));
        Ticket.getInstance().getPlatform().sendRedisMessage(new Gson().toJson(jsonObject));
    }
}