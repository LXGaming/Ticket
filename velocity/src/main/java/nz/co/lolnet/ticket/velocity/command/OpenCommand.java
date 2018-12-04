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

package nz.co.lolnet.ticket.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import nz.co.lolnet.location.api.Location;
import nz.co.lolnet.ticket.api.data.LocationData;
import nz.co.lolnet.ticket.api.data.TicketData;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.common.command.AbstractCommand;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.velocity.VelocityPlugin;
import nz.co.lolnet.ticket.velocity.util.VelocityToolbox;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public class OpenCommand extends AbstractCommand {
    
    public OpenCommand() {
        addAlias("open");
        setPermission("ticket.command.open");
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
        if (arguments.size() < 3) { // TODO Make configurable.
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Message is too short", TextColor.RED)));
            return;
        }
        
        String message = String.join(" ", arguments);
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
        
        if (!source.hasPermission("ticket.bypass.limit")) {
            Set<TicketData> tickets = DataManager.getCachedOpenTickets(user.getUniqueId());
            if (tickets.size() >= 3) { // TODO Make configurable.
                source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("You have too many open tickets", TextColor.RED)));
                return;
            }
            
            long time = System.currentTimeMillis() - 30000L; // TODO Make configurable.
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
                location.setDimension(locationUser.getDimension());
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
        
        source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("You opened a ticket, it has been assigned ID #" + ticket.getId(), TextColor.YELLOW)));
    }
}