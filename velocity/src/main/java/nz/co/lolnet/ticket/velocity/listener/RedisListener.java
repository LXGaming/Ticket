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

package nz.co.lolnet.ticket.velocity.listener;

import com.google.gson.JsonObject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import nz.co.lolnet.redisvelocity.api.event.RedisMessageEvent;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.TicketData;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.api.util.Reference;
import nz.co.lolnet.ticket.common.TicketImpl;
import nz.co.lolnet.ticket.common.configuration.Config;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.util.Toolbox;
import nz.co.lolnet.ticket.velocity.VelocityPlugin;
import nz.co.lolnet.ticket.velocity.util.VelocityToolbox;
import org.apache.commons.lang3.StringUtils;

public class RedisListener {
    
    @Subscribe
    public void onRedisMessage(RedisMessageEvent event) {
        if (StringUtils.isBlank(event.getChannel()) || !event.getChannel().equals(Reference.ID)) {
            return;
        }
        
        JsonObject jsonObject = Toolbox.parseJson(event.getMessage(), JsonObject.class).orElse(null);
        if (jsonObject == null) {
            Ticket.getInstance().getLogger().warn("Failed to parse redis message");
            return;
        }
        
        String id = Toolbox.parseJson(jsonObject.get("id"), String.class).orElse(null);
        if (StringUtils.isBlank(id) || StringUtils.equals(id, TicketImpl.getInstance().getConfig().map(Config::getProxyId).orElse(null))) {
            return;
        }
        
        String type = Toolbox.parseJson(jsonObject.get("type"), String.class).orElse(null);
        if (StringUtils.isBlank(type)) {
            Ticket.getInstance().getLogger().warn("Received invalid redis message");
        } else if (type.equals("TicketClose")) {
            TicketData ticket = Toolbox.parseJson(jsonObject.get("ticket"), TicketData.class).orElse(null);
            UserData user = Toolbox.parseJson(jsonObject.get("user"), UserData.class).orElse(null);
            if (ticket != null && user != null) {
                onTicketClose(ticket, user);
            }
        } else if (type.equals("TicketComment")) {
            TicketData ticket = Toolbox.parseJson(jsonObject.get("ticket"), TicketData.class).orElse(null);
            UserData user = Toolbox.parseJson(jsonObject.get("user"), UserData.class).orElse(null);
            if (ticket != null && user != null) {
                onTicketComment(ticket, user);
            }
        } else if (type.equals("TicketOpen")) {
            TicketData ticket = Toolbox.parseJson(jsonObject.get("ticket"), TicketData.class).orElse(null);
            UserData user = Toolbox.parseJson(jsonObject.get("user"), UserData.class).orElse(null);
            if (ticket != null && user != null) {
                onTicketOpen(ticket, user);
            }
        } else if (type.equals("UserBan")) {
            UserData user = Toolbox.parseJson(jsonObject.get("user"), UserData.class).orElse(null);
            String source = Toolbox.parseJson(jsonObject.get("by"), String.class).orElse("Unknown");
            if (user != null && StringUtils.isNotBlank(source)) {
                onUserBan(user, source);
            }
        } else if (type.equals("UserPardon")) {
            UserData user = Toolbox.parseJson(jsonObject.get("user"), UserData.class).orElse(null);
            String source = Toolbox.parseJson(jsonObject.get("by"), String.class).orElse("Unknown");
            if (user != null && StringUtils.isNotBlank(source)) {
                onUserPardon(user, source);
            }
        }
    }
    
    private void onTicketClose(TicketData ticket, UserData user) {
        DataManager.getTicketCache().put(ticket.getId(), ticket);
        TextComponent textComponent = VelocityToolbox.getTextPrefix()
                .append(TextComponent.of("Ticket #" + ticket.getId() + " was closed by ", TextColor.GOLD))
                .append(TextComponent.of(user.getName(), TextColor.YELLOW));
        
        Player player = VelocityPlugin.getInstance().getProxy().getPlayer(ticket.getUser()).orElse(null);
        if (player != null) {
            player.sendMessage(textComponent);
        }
        
        VelocityToolbox.broadcast(player, "ticket.close.notify", textComponent);
    }
    
    private void onTicketComment(TicketData ticket, UserData user) {
        DataManager.getTicketCache().put(ticket.getId(), ticket);
        DataManager.getUserCache().put(user.getUniqueId(), user);
        TextComponent textComponent = VelocityToolbox.getTextPrefix()
                .append(TextComponent.of(user.getName(), TextColor.YELLOW))
                .append(TextComponent.of(" added a comment to Ticket #" + ticket.getId(), TextColor.GOLD));
        
        Player player = VelocityPlugin.getInstance().getProxy().getPlayer(ticket.getUser()).orElse(null);
        if (player != null) {
            player.sendMessage(textComponent);
        }
        
        VelocityToolbox.broadcast(player, "ticket.open.notify", textComponent);
    }
    
    private void onTicketOpen(TicketData ticket, UserData user) {
        DataManager.getTicketCache().put(ticket.getId(), ticket);
        DataManager.getUserCache().put(user.getUniqueId(), user);
        VelocityToolbox.broadcast(null, "ticket.open.notify", VelocityToolbox.getTextPrefix()
                .append(TextComponent.of("A new ticket has been opened by ", TextColor.GREEN))
                .append(TextComponent.of(user.getName(), TextColor.YELLOW))
                .append(TextComponent.of(", id assigned #" + ticket.getId(), TextColor.GREEN)));
    }
    
    private void onUserBan(UserData user, String source) {
        DataManager.getUserCache().put(user.getUniqueId(), user);
        VelocityToolbox.broadcast(null, "ticket.ban.notify", VelocityToolbox.getTextPrefix()
                .append(TextComponent.of(user.getName(), TextColor.YELLOW))
                .append(TextComponent.of(" was banned by ", TextColor.GREEN))
                .append(TextComponent.of(source, TextColor.YELLOW)));
    }
    
    private void onUserPardon(UserData user, String source) {
        DataManager.getUserCache().put(user.getUniqueId(), user);
        VelocityToolbox.broadcast(null, "ticket.pardon.notify", VelocityToolbox.getTextPrefix()
                .append(TextComponent.of(user.getName(), TextColor.YELLOW))
                .append(TextComponent.of(" was pardoned by ", TextColor.GREEN))
                .append(TextComponent.of(source, TextColor.YELLOW)));
    }
}