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
import io.github.lxgaming.ticket.api.util.Reference;
import io.github.lxgaming.ticket.common.command.AbstractCommand;
import io.github.lxgaming.ticket.common.manager.CommandManager;
import io.github.lxgaming.ticket.velocity.util.VelocityToolbox;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class HelpCommand extends AbstractCommand {
    
    public HelpCommand() {
        addAlias("help");
        addAlias("?");
        setDescription("Helpful Information");
        setPermission("ticket.help.base");
    }
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSource source = (CommandSource) object;
        
        source.sendMessage(VelocityToolbox.getTextPrefix());
        for (AbstractCommand command : CommandManager.getCommands()) {
            if (command == this || !(StringUtils.isNotBlank(command.getPermission()) && source.hasPermission(command.getPermission()))) {
                continue;
            }
            
            TextComponent.Builder textBuilder = TextComponent.builder("");
            textBuilder.clickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, "/" + Reference.ID + " " + command.getPrimaryAlias().orElse("unknown")));
            textBuilder.hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, buildDescription(command)));
            textBuilder.append(TextComponent.of("> ", TextColor.BLUE));
            textBuilder.append(TextComponent.of("/" + Reference.ID + " " + command.getPrimaryAlias().orElse("unknown"), TextColor.GREEN));
            if (StringUtils.isNotBlank(command.getUsage())) {
                textBuilder.append(TextComponent.of(" " + command.getUsage(), TextColor.GREEN));
            }
            
            source.sendMessage(textBuilder.build());
        }
    }
    
    private TextComponent buildDescription(AbstractCommand command) {
        TextComponent.Builder textBuilder = TextComponent.builder("");
        textBuilder.append(TextComponent.of("Command: ", TextColor.AQUA));
        textBuilder.append(TextComponent.of(StringUtils.capitalize(command.getPrimaryAlias().orElse("unknown")), TextColor.DARK_GREEN));
        textBuilder.append(TextComponent.newline());
        textBuilder.append(TextComponent.of("Description: ", TextColor.AQUA));
        textBuilder.append(TextComponent.of(StringUtils.defaultIfEmpty(command.getDescription(), "No description provided"), TextColor.DARK_GREEN));
        textBuilder.append(TextComponent.newline());
        textBuilder.append(TextComponent.of("Usage: ", TextColor.AQUA));
        textBuilder.append(TextComponent.of("/" + Reference.ID + " " + command.getPrimaryAlias().orElse("unknown"), TextColor.DARK_GREEN));
        if (StringUtils.isNotBlank(command.getUsage())) {
            textBuilder.append(TextComponent.of(" " + command.getUsage(), TextColor.DARK_GREEN));
        }
        
        textBuilder.append(TextComponent.newline());
        textBuilder.append(TextComponent.of("Permission: ", TextColor.AQUA));
        textBuilder.append(TextComponent.of(StringUtils.defaultIfEmpty(command.getPermission(), "None"), TextColor.DARK_GREEN));
        textBuilder.append(TextComponent.newline());
        textBuilder.append(TextComponent.newline());
        textBuilder.append(TextComponent.of("Click to auto-complete.", TextColor.GRAY));
        return textBuilder.build();
    }
}