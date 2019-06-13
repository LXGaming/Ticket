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
import net.md_5.bungee.api.connection.ProxiedPlayer;
import io.github.lxgaming.location.api.Location;
import io.github.lxgaming.ticket.api.data.LocationData;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.bungee.BungeePlugin;
import io.github.lxgaming.ticket.bungee.util.BungeeToolbox;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.command.AbstractCommand;
import io.github.lxgaming.ticket.common.configuration.Config;
import io.github.lxgaming.ticket.common.configuration.Configuration;
import io.github.lxgaming.ticket.common.configuration.category.TicketCategory;
import io.github.lxgaming.ticket.common.manager.DataManager;
import io.github.lxgaming.ticket.common.util.Toolbox;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public class OpenCommand extends AbstractCommand {
    
    public OpenCommand() {
        addAlias("open");
        setDescription("Creates a ticket");
        setPermission("ticket.open.base");
        setUsage("<Message>");
    }
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSender sender = (CommandSender) object;
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("This command can only be executed by players.").color(ChatColor.RED).create());
            return;
        }
        
        ProxiedPlayer player = (ProxiedPlayer) sender;
        if (arguments.size() < TicketImpl.getInstance().getConfig().map(Config::getTicket).map(TicketCategory::getMinimumWords).orElse(0)) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Message is too short").color(ChatColor.RED).create());
            return;
        }
        
        String message = Toolbox.convertColor(String.join(" ", arguments));
        
        // Minecraft chat character limit
        // https://wiki.vg/Protocol#Chat_Message_.28serverbound.29
        if (message.length() > 256) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Message length may not exceed 256").color(ChatColor.RED).create());
            return;
        }
        
        UserData user = DataManager.getOrCreateUser(player.getUniqueId()).orElse(null);
        if (user == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
            return;
        }
        
        if (user.isBanned()) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You have been banned").color(ChatColor.RED).create());
            return;
        }
        
        Collection<TicketData> tickets = DataManager.getCachedOpenTickets(user.getUniqueId());
        if (!sender.hasPermission("ticket.open.exempt.max")) {
            if (tickets.size() >= TicketImpl.getInstance().getConfig().map(Config::getTicket).map(TicketCategory::getMaximumTickets).orElse(0)) {
                sender.sendMessage(BungeeToolbox.getTextPrefix().append("You have too many open tickets").color(ChatColor.RED).create());
                return;
            }
        }
        
        if (!sender.hasPermission("ticket.open.exempt.cooldown")) {
            long time = System.currentTimeMillis() - TicketImpl.getInstance().getConfig().map(Config::getTicket).map(TicketCategory::getOpenDelay).orElse(0L);
            for (TicketData ticket : tickets) {
                long duration = ticket.getTimestamp().minusMillis(time).toEpochMilli();
                if (duration > 0) {
                    sender.sendMessage(BungeeToolbox.getTextPrefix().append("You need to wait " + (duration / 1000) + " seconds before opening another ticket").color(ChatColor.RED).create());
                    return;
                }
            }
        }
        
        LocationData location = new LocationData();
        if (BungeePlugin.getInstance().getProxy().getPluginManager().getPlugin("Location") != null) {
            Location.getInstance().getUser(user.getUniqueId()).ifPresent(locationUser -> {
                location.setX(locationUser.getX());
                location.setY(locationUser.getY());
                location.setZ(locationUser.getZ());
                location.setDimension(locationUser.getDimension());
                location.setServer(locationUser.getServer());
            });
        } else {
            location.setServer(player.getServer().getInfo().getName());
        }
        
        TicketData ticket = DataManager.createTicket(user.getUniqueId(), Instant.now(), location, message).orElse(null);
        if (ticket == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
            return;
        }
        
        BungeeToolbox.sendRedisMessage("TicketOpen", jsonObject -> {
            jsonObject.add("ticket", Configuration.getGson().toJsonTree(ticket));
            jsonObject.add("user", Configuration.getGson().toJsonTree(user));
        });
        
        sender.sendMessage(BungeeToolbox.getTextPrefix().append("You opened a ticket, it has been assigned ID #" + ticket.getId()).color(ChatColor.GOLD).create());
        BungeeToolbox.broadcast(sender, "ticket.open.notify", BungeeToolbox.getTextPrefix()
                .append("A new ticket has been opened by ").color(ChatColor.GREEN)
                .append(user.getName()).color(ChatColor.YELLOW)
                .append(", id assigned #" + ticket.getId()).color(ChatColor.GREEN).create());
    }
}