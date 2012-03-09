/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.ircbot.wiki;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.internal.wiki.BotData;
import org.xwiki.ircbot.internal.wiki.BotListenerData;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

@Role
public interface WikiIRCModel
{
    XWikiContext getXWikiContext() throws IRCBotException;

    XWikiDocument getDocument(DocumentReference reference) throws IRCBotException;

    XWikiDocument getConfigurationDocument() throws IRCBotException;

    BotData loadBotData() throws IRCBotException;

    /**
     * @return the Bot Listener data for all documents containing {@link WikiIRCBotConstants#WIKI_BOT_LISTENER_CLASS}
     *         objects in the current wiki
     * @throws IRCBotException if we fail in searching the wiki
     */
    List<BotListenerData> getWikiBotListenerData() throws IRCBotException;
}
