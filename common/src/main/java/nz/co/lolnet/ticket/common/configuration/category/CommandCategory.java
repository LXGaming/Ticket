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

package nz.co.lolnet.ticket.common.configuration.category;

public class CommandCategory {
    
    private boolean legacy = false;
    private String closeTicket = "complete";
    private String commentTicket = "comment";
    private String openTicket = "modreq";
    private String readTicket = "check";
    private String reopenTicket = "reopen";
    
    public boolean isLegacy() {
        return legacy;
    }
    
    public String getCloseTicket() {
        return closeTicket;
    }
    
    public String getCommentTicket() {
        return commentTicket;
    }
    
    public String getOpenTicket() {
        return openTicket;
    }
    
    public String getReadTicket() {
        return readTicket;
    }
    
    public String getReopenTicket() {
        return reopenTicket;
    }
}