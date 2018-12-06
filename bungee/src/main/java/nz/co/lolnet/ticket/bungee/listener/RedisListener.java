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

package nz.co.lolnet.ticket.bungee.listener;

import com.google.gson.JsonObject;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.TicketData;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.api.util.Reference;
import nz.co.lolnet.ticket.bungee.BungeePlugin;
import nz.co.lolnet.ticket.bungee.util.BungeeToolbox;
import nz.co.lolnet.ticket.common.TicketImpl;
import nz.co.lolnet.ticket.common.configuration.Config;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.util.Toolbox;
import org.apache.commons.lang3.StringUtils;

public class RedisListener implements Listener {
    
    @EventHandler
    public void onPubSubMessage(PubSubMessageEvent event) {
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
        } else if (type.equals("TicketReopen")) {
            TicketData ticket = Toolbox.parseJson(jsonObject.get("ticket"), TicketData.class).orElse(null);
            String sender = Toolbox.parseJson(jsonObject.get("by"), String.class).orElse("Unknown");
            if (ticket != null && StringUtils.isNotBlank(sender)) {
                onTicketReopen(ticket, sender);
            }
        } else if (type.equals("UserBan")) {
            UserData user = Toolbox.parseJson(jsonObject.get("user"), UserData.class).orElse(null);
            String sender = Toolbox.parseJson(jsonObject.get("by"), String.class).orElse("Unknown");
            if (user != null && StringUtils.isNotBlank(sender)) {
                onUserBan(user, sender);
            }
        } else if (type.equals("UserPardon")) {
            UserData user = Toolbox.parseJson(jsonObject.get("user"), UserData.class).orElse(null);
            String sender = Toolbox.parseJson(jsonObject.get("by"), String.class).orElse("Unknown");
            if (user != null && StringUtils.isNotBlank(sender)) {
                onUserPardon(user, sender);
            }
        }
    }
    
    private void onTicketClose(TicketData ticket, UserData user) {
        DataManager.getTicketCache().put(ticket.getId(), ticket);
        BaseComponent[] baseComponents = BungeeToolbox.getTextPrefix()
                .append("Ticket #" + ticket.getId() + " was closed by ").color(ChatColor.GOLD)
                .append(user.getName()).color(ChatColor.YELLOW).create();
        
        ProxiedPlayer player = BungeePlugin.getInstance().getProxy().getPlayer(ticket.getUser());
        if (player != null) {
            player.sendMessage(baseComponents);
        }
        
        BungeeToolbox.broadcast(player, "ticket.close.notify", baseComponents);
    }
    
    private void onTicketComment(TicketData ticket, UserData user) {
        DataManager.getTicketCache().put(ticket.getId(), ticket);
        DataManager.getUserCache().put(user.getUniqueId(), user);
        BaseComponent[] baseComponents = BungeeToolbox.getTextPrefix()
                .append(user.getName()).color(ChatColor.YELLOW)
                .append(" added a comment to Ticket #" + ticket.getId()).color(ChatColor.GOLD).create();
        
        ProxiedPlayer player = BungeePlugin.getInstance().getProxy().getPlayer(ticket.getUser());
        if (player != null) {
            player.sendMessage(baseComponents);
        }
        
        BungeeToolbox.broadcast(player, "ticket.comment.notify", baseComponents);
    }
    
    private void onTicketOpen(TicketData ticket, UserData user) {
        DataManager.getTicketCache().put(ticket.getId(), ticket);
        DataManager.getUserCache().put(user.getUniqueId(), user);
        BungeeToolbox.broadcast(null, "ticket.open.notify", BungeeToolbox.getTextPrefix()
                .append("A new ticket has been opened by ").color(ChatColor.GREEN)
                .append(user.getName()).color(ChatColor.YELLOW)
                .append(", id assigned #" + ticket.getId()).color(ChatColor.GREEN).create());
    }
    
    private void onTicketReopen(TicketData ticket, String sender) {
        DataManager.getTicketCache().put(ticket.getId(), ticket);
        BungeeToolbox.broadcast(null, "ticket.reopen.notify", BungeeToolbox.getTextPrefix()
                .append("Ticket #" + ticket.getId() + " was reopened by ").color(ChatColor.GOLD)
                .append(sender).color(ChatColor.YELLOW).create());
    }
    
    private void onUserBan(UserData user, String sender) {
        DataManager.getUserCache().put(user.getUniqueId(), user);
        BungeeToolbox.broadcast(null, "ticket.ban.notify", BungeeToolbox.getTextPrefix()
                .append(user.getName()).color(ChatColor.YELLOW)
                .append(" was banned by ").color(ChatColor.GREEN)
                .append(sender).color(ChatColor.YELLOW).create());
    }
    
    private void onUserPardon(UserData user, String sender) {
        DataManager.getUserCache().put(user.getUniqueId(), user);
        BungeeToolbox.broadcast(null, "ticket.pardon.notify", BungeeToolbox.getTextPrefix()
                .append(user.getName()).color(ChatColor.YELLOW)
                .append(" was pardoned by ").color(ChatColor.GREEN)
                .append(sender).color(ChatColor.YELLOW).create());
    }
}