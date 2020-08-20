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

package io.github.lxgaming.ticket.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import io.github.lxgaming.location.api.Location;
import io.github.lxgaming.ticket.api.data.LocationData;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.command.AbstractCommand;
import io.github.lxgaming.ticket.common.configuration.Config;
import io.github.lxgaming.ticket.common.configuration.Configuration;
import io.github.lxgaming.ticket.common.configuration.category.TicketCategory;
import io.github.lxgaming.ticket.common.manager.DataManager;
import io.github.lxgaming.ticket.common.util.Toolbox;
import io.github.lxgaming.ticket.velocity.VelocityPlugin;
import io.github.lxgaming.ticket.velocity.util.VelocityToolbox;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

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
        CommandSource source = (CommandSource) object;
        if (!(source instanceof Player)) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("This command can only be executed by players.", TextColor.RED)));
            return;
        }
        
        Player player = (Player) source;
        if (arguments.size() < TicketImpl.getInstance().getConfig().map(Config::getTicket).map(TicketCategory::getMinimumWords).orElse(0)) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Message is too short", TextColor.RED)));
            return;
        }
        
        String message = Toolbox.convertColor(String.join(" ", arguments));
        
        // Minecraft chat character limit
        // https://wiki.vg/Protocol#Chat_Message_.28serverbound.29
        if (message.length() > 256) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Message length may not exceed 256", TextColor.RED)));
            return;
        }
        
        UserData user = DataManager.getOrCreateUser(player.getUniqueId()).orElse(null);
        if (user == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("An error has occurred. Details are available in console.", TextColor.RED)));
            return;
        }
        
        if (user.isBanned()) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("You have been banned", TextColor.RED)));
            return;
        }
        
        Collection<TicketData> tickets = DataManager.getCachedOpenTickets(user.getUniqueId());
        if (!source.hasPermission("ticket.open.exempt.max")) {
            if (tickets.size() >= TicketImpl.getInstance().getConfig().map(Config::getTicket).map(TicketCategory::getMaximumTickets).orElse(0)) {
                source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("You have too many open tickets", TextColor.RED)));
                return;
            }
        }
        
        if (!source.hasPermission("ticket.open.exempt.cooldown")) {
            long time = System.currentTimeMillis() - TicketImpl.getInstance().getConfig().map(Config::getTicket).map(TicketCategory::getOpenDelay).orElse(0L);
            for (TicketData ticket : tickets) {
                long duration = ticket.getTimestamp().minusMillis(time).toEpochMilli();
                if (duration > 0) {
                    source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("You need to wait " + (duration / 1000) + " seconds before opening another ticket", TextColor.RED)));
                    return;
                }
            }
        }
        
        LocationData location = new LocationData();
        if (VelocityPlugin.getInstance().getProxy().getPluginManager().isLoaded("location")) {
            Location.getInstance().getUser(user.getUniqueId()).ifPresent(locationUser -> {
                location.setX(locationUser.getX());
                location.setY(locationUser.getY());
                location.setZ(locationUser.getZ());
                if (locationUser.getDimension() != null) {
                    location.setDimension(locationUser.getDimension().getId());
                }
                
                location.setServer(locationUser.getServer());
            });
        } else {
            player.getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).ifPresent(location::setServer);
        }
        
        TicketData ticket = DataManager.createTicket(user.getUniqueId(), Instant.now(), location, message).orElse(null);
        if (ticket == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("An error has occurred. Details are available in console.", TextColor.RED)));
            return;
        }
        
        VelocityToolbox.sendRedisMessage("TicketOpen", jsonObject -> {
            jsonObject.add("ticket", Configuration.getGson().toJsonTree(ticket));
            jsonObject.add("user", Configuration.getGson().toJsonTree(user));
        });
        
        source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("You opened a ticket, it has been assigned ID #" + ticket.getId(), TextColor.GOLD)));
        VelocityToolbox.broadcast(source, "ticket.open.notify", VelocityToolbox.getTextPrefix()
                .append(TextComponent.of("A new ticket has been opened by ", TextColor.GREEN))
                .append(TextComponent.of(user.getName(), TextColor.YELLOW))
                .append(TextComponent.of(", id assigned #" + ticket.getId(), TextColor.GREEN)));
    }
}