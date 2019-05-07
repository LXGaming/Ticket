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
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.CommentData;
import nz.co.lolnet.ticket.api.data.TicketData;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.api.util.Reference;
import nz.co.lolnet.ticket.common.TicketImpl;
import nz.co.lolnet.ticket.common.command.AbstractCommand;
import nz.co.lolnet.ticket.common.configuration.Config;
import nz.co.lolnet.ticket.common.configuration.category.TicketCategory;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.util.Toolbox;
import nz.co.lolnet.ticket.velocity.util.VelocityToolbox;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;

public class ReadCommand extends AbstractCommand {
    
    public ReadCommand() {
        addAlias("read");
        addAlias("check");
        setDescription("Lists Open, Unread tickets or provides details of a specific ticket");
        setPermission("ticket.read.base");
        setUsage("[Id]");
    }
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSource source = (CommandSource) object;
        if (arguments.isEmpty()) {
            Collection<TicketData> openTickets = DataManager.getCachedOpenTickets();
            openTickets.removeIf(ticket -> {
                return !VelocityToolbox.getUniqueId(source).equals(ticket.getUser()) && !source.hasPermission("ticket.read.others");
            });
            
            if (!openTickets.isEmpty()) {
                source.sendMessage(TextComponent.builder("")
                        .append(TextComponent.of("----------", TextColor.GREEN).decoration(TextDecoration.STRIKETHROUGH, true))
                        .append(TextComponent.of(" " + openTickets.size(), TextColor.YELLOW).decoration(TextDecoration.STRIKETHROUGH, false))
                        .append(TextComponent.of(" Open " + Toolbox.formatUnit(openTickets.size(), "Ticket", "Tickets") + " ", TextColor.GREEN))
                        .append(TextComponent.of("----------", TextColor.GREEN).decoration(TextDecoration.STRIKETHROUGH, true))
                        .build());
                
                openTickets.forEach(ticket -> source.sendMessage(buildTicket(ticket)));
            }
            
            Collection<TicketData> unreadTickets = DataManager.getCachedUnreadTickets(VelocityToolbox.getUniqueId(source));
            if (!unreadTickets.isEmpty()) {
                source.sendMessage(TextComponent.builder("")
                        .append(TextComponent.of("----------", TextColor.GREEN).decoration(TextDecoration.STRIKETHROUGH, true))
                        .append(TextComponent.of(" " + unreadTickets.size(), TextColor.YELLOW).decoration(TextDecoration.STRIKETHROUGH, false))
                        .append(TextComponent.of(" Unread " + Toolbox.formatUnit(unreadTickets.size(), "Ticket", "Tickets") + " ", TextColor.GREEN))
                        .append(TextComponent.of("----------", TextColor.GREEN).decoration(TextDecoration.STRIKETHROUGH, true))
                        .build());
                
                unreadTickets.forEach(ticket -> source.sendMessage(buildTicket(ticket)));
            }
            
            if (openTickets.isEmpty() && unreadTickets.isEmpty()) {
                source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("There are no open tickets", TextColor.YELLOW)));
            }
            
            return;
        }
        
        Integer ticketId = Toolbox.parseInteger(StringUtils.removeStart(arguments.remove(0), "#")).orElse(null);
        if (ticketId == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Failed to parse ticket id", TextColor.RED)));
            return;
        }
        
        TicketData ticket = DataManager.getTicket(ticketId).orElse(null);
        if (ticket == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Failed to find ticket", TextColor.RED)));
            return;
        }
        
        if (VelocityToolbox.getUniqueId(source).equals(ticket.getUser())) {
            ticket.setRead(true);
            if (!TicketImpl.getInstance().getStorage().getQuery().updateTicket(ticket)) {
                source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("An error has occurred. Details are available in console.", TextColor.RED)));
                return;
            }
        } else if (!source.hasPermission("ticket.read.others")) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("You are not the owner of that ticket", TextColor.RED)));
            return;
        }
        
        TextComponent.Builder textBuilder = TextComponent.builder("");
        textBuilder.append(TextComponent.of("----------", TextColor.GREEN).decoration(TextDecoration.STRIKETHROUGH, true));
        textBuilder.append(TextComponent.of(" Ticket #" + ticket.getId() + " ", TextColor.YELLOW).decoration(TextDecoration.STRIKETHROUGH, false));
        textBuilder.append(TextComponent.of("----------", TextColor.GREEN).decoration(TextDecoration.STRIKETHROUGH, true));
        textBuilder.append(TextComponent.newline());
        
        textBuilder.append(TextComponent.of("Time", TextColor.AQUA)).append(TextComponent.of(": ", TextColor.WHITE));
        textBuilder.append(TextComponent.of(TicketImpl.getInstance().getConfig().map(Config::getTicket).map(TicketCategory::getDateFormat).flatMap(pattern -> Toolbox.formatInstant(pattern, ticket.getTimestamp())).orElse("Unknown")));
        
        textBuilder.append(TextComponent.newline());
        textBuilder.append(TextComponent.of("Status", TextColor.AQUA)).append(TextComponent.of(": ", TextColor.WHITE));
        if (ticket.getStatus() == 0) {
            textBuilder.append(TextComponent.of("Open", TextColor.GREEN));
        } else if (ticket.getStatus() == 1) {
            textBuilder.append(TextComponent.of("Closed", TextColor.RED));
        }
        
        textBuilder.append(TextComponent.newline());
        textBuilder.append(TextComponent.of("User", TextColor.AQUA)).append(TextComponent.of(": ", TextColor.WHITE));
        UserData user = DataManager.getUser(ticket.getUser()).orElse(null);
        if (user != null) {
            if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
                textBuilder.append(TextComponent.of(user.getName(), TextColor.GREEN));
            } else {
                textBuilder.append(TextComponent.of(user.getName(), TextColor.RED));
            }
        } else {
            textBuilder.append(TextComponent.of("Unknown", TextColor.WHITE));
        }
        
        textBuilder.append(TextComponent.newline());
        textBuilder.append(TextComponent.of("Location", TextColor.AQUA)).append(TextComponent.of(": ", TextColor.WHITE));
        
        if (ticket.getLocation().getX() != null && ticket.getLocation().getY() != null && ticket.getLocation().getZ() != null) {
            textBuilder.append(TextComponent.of("" + Toolbox.formatDecimal(ticket.getLocation().getX(), 3), TextColor.WHITE)).append(TextComponent.of(", ", TextColor.GRAY));
            textBuilder.append(TextComponent.of("" + Toolbox.formatDecimal(ticket.getLocation().getY(), 3), TextColor.WHITE)).append(TextComponent.of(", ", TextColor.GRAY));
            textBuilder.append(TextComponent.of("" + Toolbox.formatDecimal(ticket.getLocation().getZ(), 3), TextColor.WHITE)).append(TextComponent.of(" @ ", TextColor.GRAY));
        }
        
        textBuilder.append(TextComponent.of(StringUtils.defaultIfBlank(ticket.getLocation().getServer(), "Unknown"), TextColor.WHITE));
        if (ticket.getLocation().getDimension() != null) {
            textBuilder.append(TextComponent.of(" (", TextColor.GRAY)).append(TextComponent.of("" + ticket.getLocation().getDimension(), TextColor.WHITE)).append(TextComponent.of(")", TextColor.GRAY));
        }
        
        textBuilder.append(TextComponent.newline());
        textBuilder.append(TextComponent.of("Message", TextColor.AQUA)).append(TextComponent.of(": " + ticket.getText(), TextColor.WHITE));
        
        if (!ticket.getComments().isEmpty()) {
            textBuilder.append(TextComponent.newline());
            textBuilder.append(TextComponent.of("Comments", TextColor.AQUA)).append(TextComponent.of(":", TextColor.WHITE));
            source.sendMessage(textBuilder.build());
            
            ticket.getComments().forEach(comment -> source.sendMessage(buildComment(comment)));
        } else {
            source.sendMessage(textBuilder.build());
        }
    }
    
    private TextComponent buildComment(CommentData comment) {
        TextComponent.Builder textBuilder = TextComponent.builder("")
                .append(TextComponent.of(Toolbox.getShortTimeString(System.currentTimeMillis() - comment.getTimestamp().toEpochMilli()), TextColor.GREEN))
                .append(TextComponent.of(" by ", TextColor.GOLD));
        
        UserData user = DataManager.getUser(comment.getUser()).orElse(null);
        if (user != null) {
            if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
                textBuilder.append(TextComponent.of(user.getName(), TextColor.GREEN));
            } else {
                textBuilder.append(TextComponent.of(user.getName(), TextColor.RED));
            }
        } else {
            textBuilder.append(TextComponent.of("Unknown", TextColor.WHITE));
        }
        
        textBuilder.append(TextComponent.of(" - ", TextColor.GOLD));
        textBuilder.append(TextComponent.of(comment.getText(), TextColor.GRAY));
        return textBuilder.build();
    }
    
    private TextComponent buildTicket(TicketData ticket) {
        TextComponent.Builder textBuilder = TextComponent.builder("")
                .clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND, "/" + Reference.ID + " read " + ticket.getId()))
                .append(TextComponent.of("#" + ticket.getId(), TextColor.GOLD))
                .append(TextComponent.of(" " + Toolbox.getShortTimeString(System.currentTimeMillis() - ticket.getTimestamp().toEpochMilli()), TextColor.GREEN))
                .append(TextComponent.of(" by ", TextColor.GOLD));
        
        UserData user = DataManager.getCachedUser(ticket.getUser()).orElse(null);
        if (user != null) {
            if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
                textBuilder.append(TextComponent.of(user.getName(), TextColor.GREEN));
            } else {
                textBuilder.append(TextComponent.of(user.getName(), TextColor.RED));
            }
        } else {
            textBuilder.append(TextComponent.of("Unknown", TextColor.WHITE));
        }
        
        textBuilder.append(TextComponent.of(" - ", TextColor.GOLD));
        textBuilder.append(TextComponent.of(Toolbox.substring(ticket.getText(), 20), TextColor.GRAY));
        return textBuilder.build();
    }
}