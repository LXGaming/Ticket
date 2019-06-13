/*
 * Copyright 2018 Alex Thomson
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

package io.github.lxgaming.ticket.bungee.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import io.github.lxgaming.ticket.api.data.CommentData;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.api.util.Reference;
import io.github.lxgaming.ticket.bungee.BungeePlugin;
import io.github.lxgaming.ticket.bungee.util.BungeeToolbox;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.command.AbstractCommand;
import io.github.lxgaming.ticket.common.configuration.Configuration;
import io.github.lxgaming.ticket.common.manager.DataManager;
import io.github.lxgaming.ticket.common.util.Toolbox;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.List;

public class CloseCommand extends AbstractCommand {
    
    public CloseCommand() {
        addAlias("close");
        setDescription("Closes the requested ticket");
        setPermission("ticket.close.base");
        setUsage("<Id> [Message]");
    }
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSender sender = (CommandSender) object;
        if (arguments.isEmpty()) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Invalid arguments: " + getUsage()).color(ChatColor.RED).create());
            return;
        }
        
        Integer ticketId = Toolbox.parseInteger(StringUtils.removeStart(arguments.remove(0), "#")).orElse(null);
        if (ticketId == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Failed to parse ticket id").color(ChatColor.RED).create());
            return;
        }
        
        TicketData ticket = DataManager.getCachedTicket(ticketId).orElse(null);
        if (ticket == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Ticket never existed or it's no longer cached").color(ChatColor.RED).create());
            return;
        }
        
        if (!BungeeToolbox.getUniqueId(sender).equals(ticket.getUser()) && !sender.hasPermission("ticket.close.others")) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You are not the owner of that ticket").color(ChatColor.RED).create());
            return;
        }
        
        if (ticket.getStatus() == 1) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Ticket is already closed").color(ChatColor.RED).create());
            return;
        }
        
        ticket.setStatus(1);
        ticket.setRead(false);
        if (!TicketImpl.getInstance().getStorage().getQuery().updateTicket(ticket)) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
            return;
        }
        
        UserData user = DataManager.getOrCreateUser(BungeeToolbox.getUniqueId(sender)).orElse(null);
        if (user == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
            return;
        }
        
        BungeeToolbox.sendRedisMessage("TicketClose", jsonObject -> {
            jsonObject.add("ticket", Configuration.getGson().toJsonTree(ticket));
            jsonObject.add("user", Configuration.getGson().toJsonTree(user));
        });
        
        BaseComponent[] baseComponents = BungeeToolbox.getTextPrefix()
                .append("Ticket #" + ticket.getId() + " was closed by ").color(ChatColor.GOLD)
                .append(user.getName()).color(ChatColor.YELLOW).create();
        
        String command = "/" + Reference.ID + " read " + ticket.getId();
        
        if (arguments.isEmpty()) {
            // Forces the expiry to be recalculated
            DataManager.getCachedTicket(ticketId);
            ProxiedPlayer player = BungeePlugin.getInstance().getProxy().getPlayer(ticket.getUser());
            if (player != null) {
                player.sendMessage(baseComponents);
                player.sendMessage(BungeeToolbox.getTextPrefix()
                        .append("Use ").color(ChatColor.GOLD)
                        .append(command).color(ChatColor.GREEN).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .append(" to view your ticket").color(ChatColor.GOLD).create());
            }
            
            BungeeToolbox.broadcast(player, "ticket.close.notify", baseComponents);
            return;
        }
        
        String message = Toolbox.convertColor(String.join(" ", arguments));
        if (message.length() > 256) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Message length may not exceed 256").color(ChatColor.RED).create());
            return;
        }
        
        CommentData comment = DataManager.createComment(ticket.getId(), user.getUniqueId(), Instant.now(), message).orElse(null);
        if (comment == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
            return;
        }
        
        BungeeToolbox.sendRedisMessage("TicketComment", jsonObject -> {
            jsonObject.add("ticket", Configuration.getGson().toJsonTree(ticket));
            jsonObject.add("user", Configuration.getGson().toJsonTree(user));
        });
        
        ProxiedPlayer player = BungeePlugin.getInstance().getProxy().getPlayer(ticket.getUser());
        if (player != null) {
            player.sendMessage(baseComponents);
            player.sendMessage(BungeeToolbox.getTextPrefix()
                    .append("Use ").color(ChatColor.GOLD)
                    .append(command).color(ChatColor.GREEN).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                    .append(" to view your ticket").color(ChatColor.GOLD).create());
        }
        
        BungeeToolbox.broadcast(player, "ticket.close.notify", baseComponents);
    }
}