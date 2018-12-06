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

package nz.co.lolnet.ticket.bungee.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import nz.co.lolnet.ticket.api.Platform;
import nz.co.lolnet.ticket.api.util.Reference;
import nz.co.lolnet.ticket.bungee.BungeePlugin;
import nz.co.lolnet.ticket.common.TicketImpl;
import nz.co.lolnet.ticket.common.configuration.Config;
import nz.co.lolnet.ticket.common.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;
import java.util.function.Consumer;

public class BungeeToolbox {
    
    public static ComponentBuilder getTextPrefix() {
        ComponentBuilder componentBuilder = new ComponentBuilder("");
        componentBuilder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, getPluginInformation().create()));
        componentBuilder.append("[" + Reference.NAME + "]").bold(true).color(ChatColor.BLUE);
        componentBuilder.append(" ", ComponentBuilder.FormatRetention.NONE);
        return componentBuilder;
    }
    
    public static ComponentBuilder getPluginInformation() {
        ComponentBuilder componentBuilder = new ComponentBuilder("")
                .append(Reference.NAME).color(ChatColor.BLUE).bold(true).append("\n")
                .append("    Version: ", ComponentBuilder.FormatRetention.NONE).color(ChatColor.DARK_GRAY).append(Reference.VERSION).color(ChatColor.WHITE).append("\n")
                .append("    Authors: ", ComponentBuilder.FormatRetention.NONE).color(ChatColor.DARK_GRAY).append(Reference.AUTHORS).color(ChatColor.WHITE).append("\n")
                .append("    Source: ", ComponentBuilder.FormatRetention.NONE).color(ChatColor.DARK_GRAY).append(getURLClickEvent(Reference.SOURCE).create()).append("\n")
                .append("    Website: ", ComponentBuilder.FormatRetention.NONE).color(ChatColor.DARK_GRAY).append(getURLClickEvent(Reference.WEBSITE).create());
        return componentBuilder;
    }
    
    public static ComponentBuilder getURLClickEvent(String url) {
        ComponentBuilder componentBuilder = new ComponentBuilder("");
        componentBuilder.event(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        componentBuilder.append(url).color(ChatColor.BLUE);
        componentBuilder.append(" ", ComponentBuilder.FormatRetention.NONE);
        return componentBuilder;
    }
    
    public static void sendRedisMessage(String type, Consumer<JsonObject> consumer) {
        if (BungeePlugin.getInstance().getProxy().getPluginManager().getPlugin("RedisBungee") != null) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", TicketImpl.getInstance().getConfig().map(Config::getProxyId).orElse(null));
            jsonObject.addProperty("type", type);
            consumer.accept(jsonObject);
            RedisBungee.getApi().sendChannelMessage(Reference.ID, new Gson().toJson(jsonObject));
        }
    }
    
    public static void broadcast(CommandSender sender, String permission, BaseComponent[] message) {
        BungeePlugin.getInstance().getProxy().getConsole().sendMessage(message);
        for (ProxiedPlayer player : BungeePlugin.getInstance().getProxy().getPlayers()) {
            if (player != sender && (StringUtils.isBlank(permission) || player.hasPermission(permission))) {
                player.sendMessage(message);
            }
        }
    }
    
    public static UUID getUniqueId(CommandSender sender) {
        if (sender instanceof ProxiedPlayer) {
            return ((ProxiedPlayer) sender).getUniqueId();
        } else {
            return Platform.CONSOLE_UUID;
        }
    }
}