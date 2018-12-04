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
import net.kyori.text.Components;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.CommentData;
import nz.co.lolnet.ticket.api.data.TicketData;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.common.command.AbstractCommand;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.util.Toolbox;
import nz.co.lolnet.ticket.velocity.util.VelocityToolbox;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;

public class CheckCommand extends AbstractCommand {
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSource source = (CommandSource) object;
        
        if (arguments.isEmpty()) {
            Set<TicketData> tickets = DataManager.getCachedOpenTickets();
            if (tickets.isEmpty()) {
                source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("There are no open tickets", TextColor.YELLOW)));
                return;
            }
            
            for (TicketData ticket : tickets) {
                UserData user = DataManager.getCachedUser(ticket.getUser()).orElse(null);
                if (user == null) {
                    source.sendMessage(TextComponent.of("User is not cached", TextColor.RED));
                    continue;
                }
                
                TextComponent.Builder textBuilder = TextComponent.builder("")
                        .append(TextComponent.of("#" + ticket.getId(), TextColor.GOLD))
                        .append(TextComponent.of(" " + Toolbox.getShortTimeString(System.currentTimeMillis() - ticket.getTimestamp().toEpochMilli()), TextColor.GREEN))
                        .append(TextComponent.of(" by ", TextColor.GOLD));
                
                if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
                    textBuilder.append(TextComponent.of(user.getName(), TextColor.GREEN));
                } else {
                    textBuilder.append(TextComponent.of(user.getName(), TextColor.RED));
                }
                
                textBuilder.append(TextComponent.of(" - ", TextColor.GOLD));
                textBuilder.append(TextComponent.of(Toolbox.substring(ticket.getText(), 20), TextColor.GRAY));
                source.sendMessage(textBuilder.build());
            }
            
            return;
        }
        
        Integer ticketId = Toolbox.parseInteger(arguments.remove(0)).orElse(null);
        if (ticketId == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Failed to parse argument", TextColor.RED)));
            return;
        }
        
        TicketData ticket = DataManager.getTicket(ticketId).orElse(null);
        if (ticket == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Ticket doesn't exist", TextColor.RED)));
            return;
        }
        
        TextComponent.Builder textBuilder = TextComponent.builder("");
        textBuilder.append(TextComponent.of("---[", TextColor.GRAY));
        textBuilder.append(TextComponent.of("Ticket #" + ticket.getId(), TextColor.GOLD));
        textBuilder.append(TextComponent.of("]---", TextColor.GRAY));
        
        textBuilder.append(Components.newline());
        
        textBuilder.append(TextComponent.of("Status: ", TextColor.WHITE));
        
        if (ticket.getStatus() == 0) {
            textBuilder.append(TextComponent.of("Open", TextColor.GREEN));
        } else if (ticket.getStatus() == 1) {
            textBuilder.append(TextComponent.of("Closed", TextColor.RED));
        }
        
        textBuilder.append(Components.newline());
        textBuilder.append(TextComponent.of("Location: ", TextColor.WHITE));
        
        if (ticket.getLocation().getX() != null && ticket.getLocation().getY() != null && ticket.getLocation().getZ() != null) {
            textBuilder.append(TextComponent.of("" + Toolbox.formatDecimal(ticket.getLocation().getX(), 3), TextColor.GRAY));
            textBuilder.append(TextComponent.of(", ", TextColor.DARK_GRAY));
            textBuilder.append(TextComponent.of("" + Toolbox.formatDecimal(ticket.getLocation().getY(), 3), TextColor.GRAY));
            textBuilder.append(TextComponent.of(", ", TextColor.DARK_GRAY));
            textBuilder.append(TextComponent.of("" + Toolbox.formatDecimal(ticket.getLocation().getZ(), 3), TextColor.GRAY));
            textBuilder.append(TextComponent.of(" @ ", TextColor.DARK_GRAY));
        }
        
        if (ticket.getLocation().getDimension() != null && StringUtils.isNotBlank(ticket.getLocation().getServer())) {
            textBuilder.append(TextComponent.of(ticket.getLocation().getServer(), TextColor.GRAY));
            textBuilder.append(TextComponent.of(" (", TextColor.DARK_GRAY));
            textBuilder.append(TextComponent.of("" + ticket.getLocation().getDimension(), TextColor.GRAY));
            textBuilder.append(TextComponent.of(")", TextColor.DARK_GRAY));
        }
        
        textBuilder.append(Components.newline());
        textBuilder.append(TextComponent.of("Message: ", TextColor.WHITE));
        textBuilder.append(TextComponent.of(ticket.getText(), TextColor.GRAY));
        textBuilder.append(Components.newline());
        
        textBuilder.append(TextComponent.of("Comments: ", TextColor.WHITE));
        
        for (CommentData comment : ticket.getComments()) {
            UserData user = DataManager.getUser(ticket.getUser()).orElse(null);
            if (user == null) {
                // TODO Error Handling
                continue;
            }
            
            textBuilder.append(Components.newline());
            textBuilder.append(TextComponent.of(Toolbox.getShortTimeString(System.currentTimeMillis() - comment.getTimestamp().toEpochMilli()), TextColor.GRAY));
            textBuilder.append(TextComponent.of(" by ", TextColor.DARK_GRAY));
            
            if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
                textBuilder.append(TextComponent.of(user.getName(), TextColor.GREEN));
            } else {
                textBuilder.append(TextComponent.of(user.getName(), TextColor.RED));
            }
            
            textBuilder.append(TextComponent.of(": " + comment.getText(), TextColor.GRAY));
        }
        
        source.sendMessage(textBuilder.build());
    }
}