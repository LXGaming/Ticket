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

package io.github.lxgaming.ticket.velocity.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import io.github.lxgaming.redisvelocity.api.RedisVelocity;
import io.github.lxgaming.ticket.api.Platform;
import io.github.lxgaming.ticket.api.util.Reference;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.configuration.Config;
import io.github.lxgaming.ticket.velocity.VelocityPlugin;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;
import java.util.function.Consumer;

public class VelocityToolbox {
    
    public static TextComponent getTextPrefix() {
        TextComponent.Builder textBuilder = TextComponent.builder();
        textBuilder.hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, getPluginInformation()));
        textBuilder.content("[" + Reference.NAME + "]").color(TextColor.BLUE).decoration(TextDecoration.BOLD, true);
        return TextComponent.of("").append(textBuilder.build()).append(TextComponent.of(" "));
    }
    
    public static TextComponent getPluginInformation() {
        TextComponent.Builder textBuilder = TextComponent.builder("");
        textBuilder.append(TextComponent.of(Reference.NAME, TextColor.BLUE).decoration(TextDecoration.BOLD, true)).append(TextComponent.newline());
        textBuilder.append(TextComponent.of("    Version: ", TextColor.DARK_GRAY)).append(TextComponent.of(Reference.VERSION, TextColor.WHITE)).append(TextComponent.newline());
        textBuilder.append(TextComponent.of("    Authors: ", TextColor.DARK_GRAY)).append(TextComponent.of(Reference.AUTHORS, TextColor.WHITE)).append(TextComponent.newline());
        textBuilder.append(TextComponent.of("    Source: ", TextColor.DARK_GRAY)).append(getURLTextAction(Reference.SOURCE)).append(TextComponent.newline());
        textBuilder.append(TextComponent.of("    Website: ", TextColor.DARK_GRAY)).append(getURLTextAction(Reference.WEBSITE));
        return textBuilder.build();
    }
    
    public static TextComponent getURLTextAction(String url) {
        TextComponent.Builder textBuilder = TextComponent.builder();
        textBuilder.clickEvent(ClickEvent.of(ClickEvent.Action.OPEN_URL, url));
        textBuilder.content(url).color(TextColor.BLUE);
        return textBuilder.build();
    }
    
    public static void sendRedisMessage(String type, Consumer<JsonObject> consumer) {
        if (VelocityPlugin.getInstance().getProxy().getPluginManager().getPlugin("redisvelocity").isPresent()) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", TicketImpl.getInstance().getConfig().map(Config::getProxyId).orElse(null));
            jsonObject.addProperty("type", type);
            consumer.accept(jsonObject);
            RedisVelocity.getInstance().sendMessage(Reference.ID, new Gson().toJson(jsonObject));
        }
    }
    
    public static void broadcast(CommandSource source, String permission, TextComponent message) {
        VelocityPlugin.getInstance().getProxy().getConsoleCommandSource().sendMessage(message);
        for (Player player : VelocityPlugin.getInstance().getProxy().getAllPlayers()) {
            if (player != source && (StringUtils.isBlank(permission) || player.hasPermission(permission))) {
                player.sendMessage(message);
            }
        }
    }
    
    public static UUID getUniqueId(CommandSource source) {
        if (source instanceof Player) {
            return ((Player) source).getUniqueId();
        } else {
            return Platform.CONSOLE_UUID;
        }
    }
}