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

package nz.co.lolnet.ticket.bungee.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import nz.co.lolnet.ticket.api.util.Reference;
import nz.co.lolnet.ticket.bungee.util.BungeeToolbox;
import nz.co.lolnet.ticket.common.command.AbstractCommand;
import nz.co.lolnet.ticket.common.manager.CommandManager;
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
        CommandSender sender = (CommandSender) object;
        sender.sendMessage(BungeeToolbox.getTextPrefix().append("Help").color(ChatColor.GREEN).create());
        for (AbstractCommand command : CommandManager.getCommands()) {
            if (command == this || !(StringUtils.isNotBlank(command.getPermission()) && sender.hasPermission(command.getPermission()))) {
                continue;
            }
            
            ComponentBuilder componentBuilder = new ComponentBuilder("");
            componentBuilder.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + Reference.ID + " " + command.getPrimaryAlias().orElse("unknown")));
            componentBuilder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, buildDescription(command)));
            componentBuilder.append("> ").color(ChatColor.BLUE);
            componentBuilder.append("/" + Reference.ID + " " + command.getPrimaryAlias().orElse("unknown")).color(ChatColor.GREEN);
            if (StringUtils.isNotBlank(command.getUsage())) {
                componentBuilder.append(" " + command.getUsage()).color(ChatColor.GREEN);
            }
            
            sender.sendMessage(componentBuilder.create());
        }
    }
    
    private BaseComponent[] buildDescription(AbstractCommand command) {
        ComponentBuilder componentBuilder = new ComponentBuilder("");
        componentBuilder.append("Command: ").color(ChatColor.AQUA);
        componentBuilder.append(StringUtils.capitalize(command.getPrimaryAlias().orElse("unknown"))).color(ChatColor.DARK_GREEN).append("\n");
        componentBuilder.append("Description: ").color(ChatColor.AQUA);
        componentBuilder.append(StringUtils.defaultIfEmpty(command.getDescription(), "No description provided")).color(ChatColor.DARK_GREEN).append("\n");
        componentBuilder.append("Usage: ").color(ChatColor.AQUA);
        componentBuilder.append("/" + Reference.ID + " " + command.getPrimaryAlias().orElse("unknown")).color(ChatColor.DARK_GREEN);
        if (StringUtils.isNotBlank(command.getUsage())) {
            componentBuilder.append(" " + command.getUsage()).color(ChatColor.DARK_GREEN);
        }
        
        componentBuilder.append("\n");
        componentBuilder.append("Permission: ").color(ChatColor.AQUA);
        componentBuilder.append(StringUtils.defaultIfEmpty(command.getPermission(), "None")).color(ChatColor.DARK_GREEN).append("\n");
        componentBuilder.append("\n");
        componentBuilder.append("Click to auto-complete.").color(ChatColor.GRAY);
        return componentBuilder.create();
    }
}