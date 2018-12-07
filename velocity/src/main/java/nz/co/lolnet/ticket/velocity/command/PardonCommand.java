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
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.common.command.AbstractCommand;
import nz.co.lolnet.ticket.common.configuration.Configuration;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.storage.mysql.MySQLQuery;
import nz.co.lolnet.ticket.common.util.Toolbox;
import nz.co.lolnet.ticket.velocity.util.VelocityToolbox;

import java.util.List;
import java.util.UUID;

public class PardonCommand extends AbstractCommand {
    
    public PardonCommand() {
        addAlias("pardon");
        addAlias("unban");
        setDescription("Pardons a user allowing them to create Tickets");
        setPermission("ticket.pardon.base");
        setUsage("<UniqueId>");
    }
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSource source = (CommandSource) object;
        if (arguments.size() != 1) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Invalid arguments: " + getUsage(), TextColor.RED)));
            return;
        }
        
        String data = arguments.remove(0);
        if (data.length() != 36) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Invalid argument length", TextColor.RED)));
            return;
        }
        
        UUID uniqueId = Toolbox.parseUUID(data).orElse(null);
        if (uniqueId == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Failed to parse unique id", TextColor.RED)));
            return;
        }
        
        if (VelocityToolbox.getUniqueId(source).equals(uniqueId)) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("You cannot pardon yourself", TextColor.RED)));
            return;
        }
        
        UserData user = DataManager.getUser(uniqueId).orElse(null);
        if (user == null) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Failed to find user", TextColor.RED)));
            return;
        }
        
        if (!user.isBanned()) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of(user.getName(), TextColor.YELLOW)).append(TextComponent.of(" is not banned", TextColor.GREEN)));
            return;
        }
        
        user.setBanned(false);
        if (MySQLQuery.updateUser(user)) {
            VelocityToolbox.sendRedisMessage("UserPardon", jsonObject -> {
                jsonObject.add("user", Configuration.getGson().toJsonTree(user));
                jsonObject.addProperty("by", Ticket.getInstance().getPlatform().getUsername(VelocityToolbox.getUniqueId(source)).orElse("Unknown"));
            });
            
            VelocityToolbox.broadcast(null, "ticket.pardon.notify", VelocityToolbox.getTextPrefix()
                    .append(TextComponent.of(user.getName(), TextColor.YELLOW))
                    .append(TextComponent.of(" was pardoned by ", TextColor.GREEN))
                    .append(TextComponent.of(Ticket.getInstance().getPlatform().getUsername(VelocityToolbox.getUniqueId(source)).orElse("Unknown"), TextColor.YELLOW)));
        } else {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Failed to update ", TextColor.RED)).append(TextComponent.of(user.getName(), TextColor.YELLOW)));
        }
    }
}