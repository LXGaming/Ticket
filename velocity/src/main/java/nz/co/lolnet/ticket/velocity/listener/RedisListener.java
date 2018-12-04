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

package nz.co.lolnet.ticket.velocity.listener;

import com.google.gson.JsonObject;
import com.velocitypowered.api.event.Subscribe;
import nz.co.lolnet.redisvelocity.api.event.RedisMessageEvent;
import nz.co.lolnet.ticket.api.util.Reference;
import nz.co.lolnet.ticket.common.util.Toolbox;
import org.apache.commons.lang3.StringUtils;

public class RedisListener {
    
    @Subscribe
    public void onRedisMessage(RedisMessageEvent event) {
        if (StringUtils.isBlank(event.getChannel()) || !event.getChannel().equals(Reference.ID)) {
            return;
        }
        
        JsonObject jsonObject = Toolbox.parseJson(event.getMessage(), JsonObject.class).orElse(null);
        if (jsonObject == null) {
            return;
        }
        
        // TODO Redis Support
        String id = Toolbox.parseJson(jsonObject.get("event"), String.class).orElse(null);
        if (StringUtils.isBlank(id)) {
            return;
        } else if (id.equals("TicketOpenEvent")) {
        
        } else if (id.equals("TicketCloseEvent")) {
        
        }
    }
}