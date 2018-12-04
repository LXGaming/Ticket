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
import net.kyori.text.format.TextColor;
import nz.co.lolnet.ticket.api.data.CommentData;
import nz.co.lolnet.ticket.api.data.TicketData;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.common.command.AbstractCommand;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.util.Toolbox;
import nz.co.lolnet.ticket.velocity.util.VelocityToolbox;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.List;

public class CommentCommand extends AbstractCommand {
    
    public CommentCommand() {
        addAlias("comment");
        setPermission("ticket.command.comment");
        setUsage("<Id> <Message>");
    }
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSource source = (CommandSource) object;
        
        if (arguments.isEmpty()) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Invalid arguments: " + getUsage(), TextColor.RED)));
            return;
        }
        
        Integer ticketId = Toolbox.parseInteger(arguments.remove(0)).orElse(null);
        if (ticketId == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Failed to parse argument", TextColor.RED)));
            return;
        }
        
        String message = String.join(" ", arguments);
        if (StringUtils.isBlank(message)) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Message cannot be blank", TextColor.RED)));
            return;
        }
        
        if (message.length() > 256) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Message length may not exceed 256", TextColor.RED)));
            return;
        }
        
        UserData user = DataManager.getOrCreateUser(VelocityToolbox.getUniqueId(source)).orElse(null);
        if (user == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("An error has occurred. Details are available in console.", TextColor.RED)));
            return;
        }
        
        TicketData ticket = DataManager.getTicket(ticketId).orElse(null);
        if (ticket == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Ticket doesn't exist", TextColor.RED)));
            return;
        }
        
        CommentData comment = DataManager.createComment(ticket.getId(), user.getUniqueId(), Instant.now(), message).orElse(null);
        if (comment == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("An error has occurred. Details are available in console.", TextColor.RED)));
            return;
        }
        
        source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("You added a comment to ticket #" + ticket.getId(), TextColor.GOLD)));
    }
}