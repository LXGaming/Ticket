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

package nz.co.lolnet.ticket.bungee.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import nz.co.lolnet.ticket.api.data.CommentData;
import nz.co.lolnet.ticket.api.data.TicketData;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.api.util.Reference;
import nz.co.lolnet.ticket.bungee.BungeePlugin;
import nz.co.lolnet.ticket.bungee.util.BungeeToolbox;
import nz.co.lolnet.ticket.common.TicketImpl;
import nz.co.lolnet.ticket.common.command.AbstractCommand;
import nz.co.lolnet.ticket.common.configuration.Config;
import nz.co.lolnet.ticket.common.configuration.Configuration;
import nz.co.lolnet.ticket.common.configuration.category.TicketCategory;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.util.Toolbox;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.List;

public class CommentCommand extends AbstractCommand {
    
    public CommentCommand() {
        addAlias("comment");
        addAlias("comments");
        setDescription("Adds a comment to requested ticket");
        setPermission("ticket.comment.base");
        setUsage("<Id> <Message>");
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
        
        String message = Toolbox.convertColor(String.join(" ", arguments));
        if (StringUtils.isBlank(message)) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Message cannot be blank").color(ChatColor.RED).create());
            return;
        }
        
        // Minecraft chat character limit
        // https://wiki.vg/Protocol#Chat_Message_.28serverbound.29
        if (message.length() > 256) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Message length may not exceed 256").color(ChatColor.RED).create());
            return;
        }
        
        UserData user = DataManager.getOrCreateUser(BungeeToolbox.getUniqueId(sender)).orElse(null);
        if (user == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
            return;
        }
        
        if (user.isBanned()) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You have been banned").color(ChatColor.RED).create());
            return;
        }
        
        TicketData ticket = DataManager.getTicket(ticketId).orElse(null);
        if (ticket == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Ticket doesn't exist").color(ChatColor.RED).create());
            return;
        }
        
        if (!user.getUniqueId().equals(ticket.getUser()) && !sender.hasPermission("ticket.comment.others")) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You are not the owner of that ticket").color(ChatColor.RED).create());
            return;
        }
        
        if (!sender.hasPermission("ticket.comment.exempt.cooldown")) {
            long time = System.currentTimeMillis() - TicketImpl.getInstance().getConfig().map(Config::getTicket).map(TicketCategory::getCommentDelay).orElse(0L);
            for (CommentData comment : ticket.getComments()) {
                long duration = comment.getTimestamp().minusMillis(time).toEpochMilli();
                if (duration > 0) {
                    sender.sendMessage(BungeeToolbox.getTextPrefix().append("You need to wait " + (duration / 1000) + " seconds before adding another comment").color(ChatColor.RED).create());
                    return;
                }
            }
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
        
        BaseComponent[] baseComponents = BungeeToolbox.getTextPrefix()
                .append(user.getName()).color(ChatColor.YELLOW)
                .append(" added a comment to Ticket #" + ticket.getId()).color(ChatColor.GOLD).create();
        
        ProxiedPlayer player = BungeePlugin.getInstance().getProxy().getPlayer(ticket.getUser());
        if (player != null) {
            player.sendMessage(baseComponents);
            
            String command = "/" + Reference.ID + " read " + ticket.getId();
            player.sendMessage(BungeeToolbox.getTextPrefix()
                    .append("Use ").color(ChatColor.GOLD)
                    .append(command).color(ChatColor.GREEN).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                    .append(" to view your ticket").color(ChatColor.GOLD).create());
        }
        
        BungeeToolbox.broadcast(player, "ticket.comment.notify", baseComponents);
    }
}