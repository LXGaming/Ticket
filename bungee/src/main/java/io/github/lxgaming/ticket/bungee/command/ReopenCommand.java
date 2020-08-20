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

import io.github.lxgaming.ticket.api.Ticket;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.bungee.BungeePlugin;
import io.github.lxgaming.ticket.bungee.util.BungeeToolbox;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.command.AbstractCommand;
import io.github.lxgaming.ticket.common.configuration.Configuration;
import io.github.lxgaming.ticket.common.manager.DataManager;
import io.github.lxgaming.ticket.common.util.Toolbox;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ReopenCommand extends AbstractCommand {
    
    public ReopenCommand() {
        addAlias("reopen");
        setDescription("Reopens the requested ticket");
        setPermission("ticket.reopen.base");
        setUsage("<Id>");
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
        
        TicketData ticket = DataManager.getTicket(ticketId).orElse(null);
        if (ticket == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Ticket doesn't exist").color(ChatColor.RED).create());
            return;
        }
        
        if (ticket.getStatus() == 0) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Ticket is already open").color(ChatColor.RED).create());
            return;
        }
        
        ticket.setStatus(0);
        ticket.setRead(false);
        if (!TicketImpl.getInstance().getStorage().getQuery().updateTicket(ticket)) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
            return;
        }
        
        BungeeToolbox.sendRedisMessage("TicketReopen", jsonObject -> {
            jsonObject.add("ticket", Configuration.getGson().toJsonTree(ticket));
            jsonObject.addProperty("by", Ticket.getInstance().getPlatform().getUsername(BungeeToolbox.getUniqueId(sender)).orElse("Unknown"));
        });
        
        BaseComponent[] baseComponents = BungeeToolbox.getTextPrefix()
                .append("Ticket #" + ticket.getId() + " was reopened by ").color(ChatColor.GOLD)
                .append(Ticket.getInstance().getPlatform().getUsername(BungeeToolbox.getUniqueId(sender)).orElse("Unknown")).color(ChatColor.YELLOW)
                .create();
        
        ProxiedPlayer player = BungeePlugin.getInstance().getProxy().getPlayer(ticket.getUser());
        if (player != null) {
            player.sendMessage(baseComponents);
        }
        
        BungeeToolbox.broadcast(player, "ticket.reopen.notify", baseComponents);
    }
}