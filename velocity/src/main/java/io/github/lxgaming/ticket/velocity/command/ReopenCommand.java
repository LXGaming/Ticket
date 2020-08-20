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
import io.github.lxgaming.ticket.api.Ticket;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.command.AbstractCommand;
import io.github.lxgaming.ticket.common.configuration.Configuration;
import io.github.lxgaming.ticket.common.manager.DataManager;
import io.github.lxgaming.ticket.common.util.Toolbox;
import io.github.lxgaming.ticket.velocity.VelocityPlugin;
import io.github.lxgaming.ticket.velocity.util.VelocityToolbox;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
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
        
        TicketData ticket = DataManager.getTicket(ticketId).orElse(null);
        if (ticket == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Ticket doesn't exist", TextColor.RED)));
            return;
        }
        
        if (ticket.getStatus() == 0) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Ticket is already open", TextColor.RED)));
            return;
        }
        
        ticket.setStatus(0);
        ticket.setRead(false);
        if (!TicketImpl.getInstance().getStorage().getQuery().updateTicket(ticket)) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("An error has occurred. Details are available in console.", TextColor.RED)));
            return;
        }
        
        VelocityToolbox.sendRedisMessage("TicketReopen", jsonObject -> {
            jsonObject.add("ticket", Configuration.getGson().toJsonTree(ticket));
            jsonObject.addProperty("by", Ticket.getInstance().getPlatform().getUsername(VelocityToolbox.getUniqueId(source)).orElse("Unknown"));
        });
        
        TextComponent textComponent = VelocityToolbox.getTextPrefix()
                .append(TextComponent.of("Ticket #" + ticket.getId() + " was reopened by ", TextColor.GOLD))
                .append(TextComponent.of(Ticket.getInstance().getPlatform().getUsername(VelocityToolbox.getUniqueId(source)).orElse("Unknown"), TextColor.YELLOW));
        
        Player player = VelocityPlugin.getInstance().getProxy().getPlayer(ticket.getUser()).orElse(null);
        if (player != null) {
            player.sendMessage(textComponent);
        }
        
        VelocityToolbox.broadcast(player, "ticket.reopen.notify", textComponent);
    }
}