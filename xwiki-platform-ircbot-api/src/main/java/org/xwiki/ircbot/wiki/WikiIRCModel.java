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
import org.xwiki.ircbot.internal.BotData;
import org.xwiki.ircbot.internal.BotListenerData;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Provides APIs to access to the Wiki Model of the Bot (ie documents in the wiki containing Bot data).
 *
 * @version $Id$
 * @since 4.0M2
 */
@Role
public interface WikiIRCModel
{
    /**
     * @return the XWiki Context
     * @throws IRCBotException if the XWiki Context is null
     */
    XWikiContext getXWikiContext() throws IRCBotException;

    /**
     * @param reference the reference to the document to access
     * @return the document instance
     * @throws IRCBotException if the document cannot be retrieved
     */
    XWikiDocument getDocument(DocumentReference reference) throws IRCBotException;

    /**
     * @return the document instance of the Bot configuration page
     * @throws IRCBotException if the configuration document cannot be retrieved
     */
    XWikiDocument getConfigurationDocument() throws IRCBotException;

    /**
     * @return Bot configuration data (channel, server, etc)
     * @throws IRCBotException if the configuration document cannot be retrieved
     */
    BotData loadBotData() throws IRCBotException;

    /**
     * @param isActive true if the Bot is to be marked as active or false if it's to be marked as inactive
     * @throws IRCBotException if the XWiki Context is null
     */
    void setActive(boolean isActive) throws IRCBotException;

    /**
     * @return the Bot Listener data for all documents containing {@link WikiIRCBotConstants#WIKI_BOT_LISTENER_CLASS}
     *         objects in the current wiki
     * @throws IRCBotException if we fail in searching the wiki
     */
    List<BotListenerData> getWikiBotListenerData() throws IRCBotException;

    /**
     * Execute some code as the passed user and using the passed document as the current security document on which
     * permissions are verified.
     *
     * @param executingUserReference the user under which to run
     * @param securityDocumentReference the security document under which to run
     * @param executor the code to execute
     * @throws Exception if any error happens when executing the code or if the XWiki Context is null
     */
    void executeAsUser(DocumentReference executingUserReference, DocumentReference securityDocumentReference,
        Executor executor) throws Exception;

    /**
     * Generic interface to execute code.
     */
    public interface Executor
    {
        /**
         * Execute some code.
         *
         * @throws Exception if any error happens when executing the code
         */
        void execute() throws Exception;
    }
}
