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
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.api.util.Reference;
import nz.co.lolnet.ticket.common.command.AbstractCommand;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.util.Toolbox;
import nz.co.lolnet.ticket.velocity.util.VelocityToolbox;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserCommand extends AbstractCommand {
    
    public UserCommand() {
        addAlias("user");
        setDescription("Information about the requested User");
        setPermission("ticket.user.base");
        setUsage("<User>");
    }
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSource source = (CommandSource) object;
        if (arguments.size() != 1) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Invalid arguments: " + getUsage(), TextColor.RED)));
            return;
        }
        
        String data = arguments.remove(0);
        if (data.length() < 3 || (data.length() > 16 && data.length() != 36)) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Invalid argument length", TextColor.RED)));
            return;
        }
        
        if (data.length() == 36) {
            UUID uniqueId = Toolbox.parseUUID(data).orElse(null);
            if (uniqueId == null) {
                source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Failed to parse unique id", TextColor.RED)));
                return;
            }
            
            UserData user = DataManager.getUser(uniqueId).orElse(null);
            if (user == null) {
                source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Failed to find user", TextColor.RED)));
                return;
            }
            
            source.sendMessage(buildUserInfo(user));
            return;
        }
        
        Collection<UserData> users = DataManager.getUsers(data).orElse(null);
        if (users == null || users.isEmpty()) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Fail to find users by the name of ", TextColor.RED)).append(TextComponent.of(data, TextColor.YELLOW)));
            return;
        }
        
        if (users.size() == 1) {
            source.sendMessage(buildUserInfo(users.iterator().next()));
            return;
        }
        
        source.sendMessage(TextComponent.builder("")
                .append(TextComponent.of("----------", TextColor.GREEN).decoration(TextDecoration.STRIKETHROUGH, true))
                .append(TextComponent.of(" Users ", TextColor.GREEN).decoration(TextDecoration.STRIKETHROUGH, false))
                .append(TextComponent.of("----------", TextColor.GREEN).decoration(TextDecoration.STRIKETHROUGH, true))
                .build());
        for (UserData user : users) {
            TextComponent.Builder textBuilder = TextComponent.builder("");
            textBuilder.clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND, "/" + Reference.ID + " " + getPrimaryAlias().orElse("unknown") + " " + user.getUniqueId()));
            textBuilder.append(TextComponent.of("> ", TextColor.BLUE));
            
            if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
                textBuilder.append(TextComponent.of(user.getName(), TextColor.GREEN));
            } else {
                textBuilder.append(TextComponent.of(user.getName(), TextColor.RED));
            }
            
            textBuilder.append(TextComponent.of(" (", TextColor.DARK_GRAY)).append(TextComponent.of(user.getUniqueId().toString(), TextColor.GRAY)).append(TextComponent.of(")", TextColor.DARK_GRAY));
            source.sendMessage(textBuilder.build());
        }
    }
    
    private TextComponent buildUserInfo(UserData user) {
        TextComponent.Builder textBuilder = TextComponent.builder("");
        textBuilder.append(TextComponent.of("----------", TextColor.GREEN).decoration(TextDecoration.STRIKETHROUGH, true));
        textBuilder.append(TextComponent.of(" User Info: ", TextColor.GREEN).decoration(TextDecoration.STRIKETHROUGH, false));
        textBuilder.append(TextComponent.of(user.getName() + " ", TextColor.YELLOW));
        textBuilder.append(TextComponent.of("----------", TextColor.GREEN).decoration(TextDecoration.STRIKETHROUGH, true));
        textBuilder.append(TextComponent.newline());
        
        textBuilder.append(TextComponent.of("UUID", TextColor.AQUA)).append(TextComponent.of(": " + user.getUniqueId(), TextColor.WHITE));
        textBuilder.append(TextComponent.newline());
        
        textBuilder.append(TextComponent.of("Online", TextColor.AQUA)).append(TextComponent.of(": ", TextColor.WHITE));
        if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
            textBuilder.append(TextComponent.of("Yes", TextColor.GREEN));
        } else {
            textBuilder.append(TextComponent.of("No", TextColor.RED));
        }
        
        textBuilder.append(TextComponent.newline());
        textBuilder.append(TextComponent.of("Banned", TextColor.AQUA)).append(TextComponent.of(": ", TextColor.WHITE));
        if (user.isBanned()) {
            textBuilder.append(TextComponent.of("Yes", TextColor.RED));
        } else {
            textBuilder.append(TextComponent.of("No", TextColor.GREEN));
        }
        
        textBuilder.append(TextComponent.newline()).append(TextComponent.newline());
        textBuilder.append(TextComponent.builder("")
                .clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND, "/" + Reference.ID + " ban " + user.getUniqueId()))
                .append(TextComponent.of("[", TextColor.GOLD)).append(TextComponent.of("Ban", TextColor.RED)).append(TextComponent.of("]", TextColor.GOLD))
                .build());
        
        textBuilder.append(TextComponent.space());
        textBuilder.append(TextComponent.builder("")
                .clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND, "/" + Reference.ID + " pardon " + user.getUniqueId()))
                .append(TextComponent.of("[", TextColor.GOLD)).append(TextComponent.of("Pardon", TextColor.GREEN)).append(TextComponent.of("]", TextColor.GOLD))
                .build());
        
        textBuilder.append(TextComponent.of(""));
        return textBuilder.build();
    }
}