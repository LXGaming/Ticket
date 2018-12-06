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
import net.md_5.bungee.api.chat.ComponentBuilder;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.CommentData;
import nz.co.lolnet.ticket.api.data.TicketData;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.bungee.util.BungeeToolbox;
import nz.co.lolnet.ticket.common.TicketImpl;
import nz.co.lolnet.ticket.common.command.AbstractCommand;
import nz.co.lolnet.ticket.common.configuration.Config;
import nz.co.lolnet.ticket.common.configuration.category.TicketCategory;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.storage.mysql.MySQLQuery;
import nz.co.lolnet.ticket.common.util.Toolbox;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;

public class ReadCommand extends AbstractCommand {
    
    public ReadCommand() {
        addAlias("read");
        addAlias("check");
        setPermission("ticket.read.base");
        setUsage("[Id]");
    }
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSender sender = (CommandSender) object;
        if (arguments.isEmpty()) {
            Collection<TicketData> openTickets = DataManager.getCachedOpenTickets();
            openTickets.removeIf(ticket -> {
                return !BungeeToolbox.getUniqueId(sender).equals(ticket.getUser()) && !sender.hasPermission("ticket.read.others");
            });
            
            if (!openTickets.isEmpty()) {
                sender.sendMessage(new ComponentBuilder("")
                        .append("----------").color(ChatColor.GREEN).strikethrough(true)
                        .append(" " + openTickets.size()).color(ChatColor.YELLOW).strikethrough(false)
                        .append(" Open " + Toolbox.formatUnit(openTickets.size(), "Ticket", "Tickets") + " ").color(ChatColor.GREEN)
                        .append("----------").color(ChatColor.GREEN).strikethrough(true)
                        .create());
                
                openTickets.forEach(ticket -> sender.sendMessage(buildTicket(ticket)));
            }
            
            Collection<TicketData> unreadTickets = DataManager.getCachedUnreadTickets(BungeeToolbox.getUniqueId(sender));
            if (!unreadTickets.isEmpty()) {
                sender.sendMessage(new ComponentBuilder("")
                        .append("----------").color(ChatColor.GREEN).strikethrough(true)
                        .append(" " + unreadTickets.size()).color(ChatColor.YELLOW).strikethrough(false)
                        .append(" Unread " + Toolbox.formatUnit(unreadTickets.size(), "Ticket", "Tickets") + " ").color(ChatColor.GREEN)
                        .append("----------").color(ChatColor.GREEN).strikethrough(true)
                        .create());
                
                unreadTickets.forEach(ticket -> sender.sendMessage(buildTicket(ticket)));
            }
            
            if (openTickets.isEmpty() && unreadTickets.isEmpty()) {
                sender.sendMessage(BungeeToolbox.getTextPrefix().append("There are no open tickets").color(ChatColor.YELLOW).create());
            }
            
            return;
        }
        
        Integer ticketId = Toolbox.parseInteger(arguments.remove(0)).orElse(null);
        if (ticketId == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Failed to parse ticket id").color(ChatColor.RED).create());
            return;
        }
        
        TicketData ticket = DataManager.getTicket(ticketId).orElse(null);
        if (ticket == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Failed to find ticket").color(ChatColor.RED).create());
            return;
        }
        
        if (BungeeToolbox.getUniqueId(sender).equals(ticket.getUser())) {
            ticket.setRead(true);
            if (!MySQLQuery.updateTicket(ticket)) {
                sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
                return;
            }
        } else if (!sender.hasPermission("ticket.read.others")) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You are not the owner of that ticket").color(ChatColor.RED).create());
            return;
        }
        
        ComponentBuilder componentBuilder = new ComponentBuilder("");
        componentBuilder.append("----------").color(ChatColor.GREEN).strikethrough(true);
        componentBuilder.append(" Ticket #" + ticket.getId() + " ").color(ChatColor.YELLOW).strikethrough(false);
        componentBuilder.append("----------").color(ChatColor.GREEN).strikethrough(true);
        componentBuilder.append("\n", ComponentBuilder.FormatRetention.NONE);
        
        componentBuilder.append("Time").color(ChatColor.AQUA).append(": ").color(ChatColor.WHITE);
        componentBuilder.append(TicketImpl.getInstance().getConfig().map(Config::getTicket).map(TicketCategory::getDateFormat).flatMap(pattern -> Toolbox.formatInstant(pattern, ticket.getTimestamp())).orElse("Unknown"));
        
        componentBuilder.append("\n");
        componentBuilder.append("Status").color(ChatColor.AQUA).append(": ").color(ChatColor.WHITE);
        if (ticket.getStatus() == 0) {
            componentBuilder.append("Open").color(ChatColor.GREEN);
        } else if (ticket.getStatus() == 1) {
            componentBuilder.append("Closed").color(ChatColor.RED);
        }
        
        componentBuilder.append("\n");
        componentBuilder.append("User").color(ChatColor.AQUA).append(": ").color(ChatColor.WHITE);
        UserData user = DataManager.getUser(ticket.getUser()).orElse(null);
        if (user != null) {
            if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
                componentBuilder.append(user.getName()).color(ChatColor.GREEN);
            } else {
                componentBuilder.append(user.getName()).color(ChatColor.RED);
            }
        } else {
            componentBuilder.append("Unknown").color(ChatColor.WHITE);
        }
        
        componentBuilder.append("\n");
        componentBuilder.append("Location").color(ChatColor.AQUA).append(": ").color(ChatColor.WHITE);
        
        if (ticket.getLocation().getX() != null && ticket.getLocation().getY() != null && ticket.getLocation().getZ() != null) {
            componentBuilder.append("" + Toolbox.formatDecimal(ticket.getLocation().getX(), 3)).color(ChatColor.WHITE).append(", ").color(ChatColor.GRAY);
            componentBuilder.append("" + Toolbox.formatDecimal(ticket.getLocation().getY(), 3)).color(ChatColor.WHITE).append(", ").color(ChatColor.GRAY);
            componentBuilder.append("" + Toolbox.formatDecimal(ticket.getLocation().getZ(), 3)).color(ChatColor.WHITE).append(" @ ").color(ChatColor.GRAY);
        }
        
        componentBuilder.append(StringUtils.defaultIfBlank(ticket.getLocation().getServer(), "Unknown")).color(ChatColor.WHITE);
        if (ticket.getLocation().getDimension() != null) {
            componentBuilder.append(" (").color(ChatColor.GRAY).append("" + ticket.getLocation().getDimension()).color(ChatColor.WHITE).append(")").color(ChatColor.GRAY);
        }
        
        componentBuilder.append("\n");
        componentBuilder.append("Message").color(ChatColor.AQUA).append(": " + ticket.getText()).color(ChatColor.WHITE);
        if (!ticket.getComments().isEmpty()) {
            componentBuilder.append("\n");
            componentBuilder.append("Comments").color(ChatColor.AQUA).append(":").color(ChatColor.WHITE);
            sender.sendMessage(componentBuilder.create());
            
            ticket.getComments().forEach(comment -> sender.sendMessage(buildComment(comment)));
        } else {
            sender.sendMessage(componentBuilder.create());
        }
    }
    
    private BaseComponent[] buildComment(CommentData comment) {
        ComponentBuilder componentBuilder = new ComponentBuilder("");
        componentBuilder.append(Toolbox.getShortTimeString(System.currentTimeMillis() - comment.getTimestamp().toEpochMilli())).color(ChatColor.GREEN);
        componentBuilder.append(" by ").color(ChatColor.GOLD);
        
        UserData user = DataManager.getUser(comment.getUser()).orElse(null);
        if (user != null) {
            if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
                componentBuilder.append(user.getName()).color(ChatColor.GREEN);
            } else {
                componentBuilder.append(user.getName()).color(ChatColor.RED);
            }
        } else {
            componentBuilder.append("Unknown").color(ChatColor.WHITE);
        }
        
        componentBuilder.append(" - ").color(ChatColor.GOLD);
        componentBuilder.append(comment.getText()).color(ChatColor.GRAY);
        return componentBuilder.create();
    }
    
    private BaseComponent[] buildTicket(TicketData ticket) {
        ComponentBuilder componentBuilder = new ComponentBuilder("")
                .append("#" + ticket.getId()).color(ChatColor.GOLD)
                .append(" " + Toolbox.getShortTimeString(System.currentTimeMillis() - ticket.getTimestamp().toEpochMilli())).color(ChatColor.GREEN)
                .append(" by ").color(ChatColor.GOLD);
        
        UserData user = DataManager.getCachedUser(ticket.getUser()).orElse(null);
        if (user != null) {
            if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
                componentBuilder.append(user.getName()).color(ChatColor.GREEN);
            } else {
                componentBuilder.append(user.getName()).color(ChatColor.RED);
            }
        } else {
            componentBuilder.append("Unknown").color(ChatColor.WHITE);
        }
        
        componentBuilder.append(" - ").color(ChatColor.GOLD);
        componentBuilder.append(Toolbox.substring(ticket.getText(), 20)).color(ChatColor.GRAY);
        return componentBuilder.create();
    }
}