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
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import io.github.lxgaming.ticket.api.data.CommentData;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.api.util.Reference;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.command.AbstractCommand;
import io.github.lxgaming.ticket.common.configuration.Configuration;
import io.github.lxgaming.ticket.common.manager.DataManager;
import io.github.lxgaming.ticket.common.util.Toolbox;
import io.github.lxgaming.ticket.velocity.VelocityPlugin;
import io.github.lxgaming.ticket.velocity.util.VelocityToolbox;
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
        CommandSource source = (CommandSource) object;
        if (arguments.isEmpty()) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Invalid arguments: " + getUsage(), TextColor.RED)));
            return;
        }
        
        Integer ticketId = Toolbox.parseInteger(StringUtils.removeStart(arguments.remove(0), "#")).orElse(null);
        if (ticketId == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Failed to parse ticket id", TextColor.RED)));
            return;
        }
        
        TicketData ticket = DataManager.getCachedTicket(ticketId).orElse(null);
        if (ticket == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Ticket never existed or it's no longer cached", TextColor.RED)));
            return;
        }
        
        if (!VelocityToolbox.getUniqueId(source).equals(ticket.getUser()) && !source.hasPermission("ticket.close.others")) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("You are not the owner of that ticket", TextColor.RED)));
            return;
        }
        
        if (ticket.getStatus() == 1) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Ticket is already closed", TextColor.RED)));
            return;
        }
        
        ticket.setStatus(1);
        ticket.setRead(false);
        if (!TicketImpl.getInstance().getStorage().getQuery().updateTicket(ticket)) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("An error has occurred. Details are available in console.", TextColor.RED)));
            return;
        }
        
        UserData user = DataManager.getOrCreateUser(VelocityToolbox.getUniqueId(source)).orElse(null);
        if (user == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("An error has occurred. Details are available in console.", TextColor.RED)));
            return;
        }
        
        VelocityToolbox.sendRedisMessage("TicketClose", jsonObject -> {
            jsonObject.add("ticket", Configuration.getGson().toJsonTree(ticket));
            jsonObject.add("user", Configuration.getGson().toJsonTree(user));
        });
        
        TextComponent textComponent = VelocityToolbox.getTextPrefix()
                .append(TextComponent.of("Ticket #" + ticket.getId() + " was closed by ", TextColor.GOLD))
                .append(TextComponent.of(user.getName(), TextColor.YELLOW));
        
        String command = "/" + Reference.ID + " read " + ticket.getId();
        
        if (arguments.isEmpty()) {
            // Forces the expiry to be recalculated
            DataManager.getCachedTicket(ticketId);
            Player player = VelocityPlugin.getInstance().getProxy().getPlayer(ticket.getUser()).orElse(null);
            if (player != null) {
                player.sendMessage(textComponent);
                player.sendMessage(VelocityToolbox.getTextPrefix()
                        .append(TextComponent.of("Use ", TextColor.GOLD))
                        .append(TextComponent.of(command, TextColor.GREEN).clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND, command)))
                        .append(TextComponent.of(" to view your ticket", TextColor.GOLD)));
            }
            
            VelocityToolbox.broadcast(player, "ticket.close.notify", textComponent);
            return;
        }
        
        String message = Toolbox.convertColor(String.join(" ", arguments));
        if (message.length() > 256) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Message length may not exceed 256", TextColor.RED)));
            return;
        }
        
        CommentData comment = DataManager.createComment(ticket.getId(), user.getUniqueId(), Instant.now(), message).orElse(null);
        if (comment == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("An error has occurred. Details are available in console.", TextColor.RED)));
            return;
        }
        
        VelocityToolbox.sendRedisMessage("TicketComment", jsonObject -> {
            jsonObject.add("ticket", Configuration.getGson().toJsonTree(ticket));
            jsonObject.add("user", Configuration.getGson().toJsonTree(user));
        });
        
        Player player = VelocityPlugin.getInstance().getProxy().getPlayer(ticket.getUser()).orElse(null);
        if (player != null) {
            player.sendMessage(textComponent);
            player.sendMessage(VelocityToolbox.getTextPrefix()
                    .append(TextComponent.of("Use ", TextColor.GOLD))
                    .append(TextComponent.of(command, TextColor.GREEN).clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND, command)))
                    .append(TextComponent.of(" to view your ticket", TextColor.GOLD)));
        }
        
        VelocityToolbox.broadcast(player, "ticket.close.notify", textComponent);
    }
}