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
import net.md_5.bungee.api.connection.ProxiedPlayer;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.CommentData;
import nz.co.lolnet.ticket.api.data.TicketData;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.bungee.BungeePlugin;
import nz.co.lolnet.ticket.bungee.util.BungeeToolbox;
import nz.co.lolnet.ticket.common.command.AbstractCommand;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.storage.mysql.MySQLQuery;
import nz.co.lolnet.ticket.common.util.Toolbox;

import java.time.Instant;
import java.util.List;

public class CloseCommand extends AbstractCommand {
    
    public CloseCommand() {
        addAlias("close");
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
        
        Integer ticketId = Toolbox.parseInteger(arguments.remove(0)).orElse(null);
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
        if (!MySQLQuery.updateTicket(ticket)) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
            return;
        }
        
        BaseComponent[] baseComponents = BungeeToolbox.getTextPrefix()
                .append("Ticket #" + ticket.getId() + " was closed by ").color(ChatColor.GOLD)
                .append(Ticket.getInstance().getPlatform().getUsername(BungeeToolbox.getUniqueId(sender)).orElse("Unknown")).color(ChatColor.YELLOW).create();
        
        if (arguments.isEmpty()) {
            // Forces the expiry to be recalculated
            DataManager.getCachedTicket(ticketId);
            ProxiedPlayer player = BungeePlugin.getInstance().getProxy().getPlayer(ticket.getUser());
            if (player != null) {
                player.sendMessage(baseComponents);
            }
            
            BungeeToolbox.broadcast(player, "ticket.close.notify", baseComponents);
            return;
        }
        
        String message = Toolbox.convertColor(String.join(" ", arguments));
        if (message.length() > 256) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Message length may not exceed 256").color(ChatColor.RED).create());
            return;
        }
        
        UserData user = DataManager.getOrCreateUser(BungeeToolbox.getUniqueId(sender)).orElse(null);
        if (user == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
            return;
        }
        
        CommentData comment = DataManager.createComment(ticket.getId(), user.getUniqueId(), Instant.now(), message).orElse(null);
        if (comment == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
            return;
        }
        
        ProxiedPlayer player = BungeePlugin.getInstance().getProxy().getPlayer(ticket.getUser());
        if (player != null) {
            player.sendMessage(baseComponents);
        }
        
        BungeeToolbox.broadcast(player, "ticket.close.notify", baseComponents);
    }
}