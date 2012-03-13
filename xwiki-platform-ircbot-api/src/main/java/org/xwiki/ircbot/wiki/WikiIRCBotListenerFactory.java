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

import org.xwiki.component.annotation.Role;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.internal.wiki.WikiIRCBotListener;
import org.xwiki.model.reference.DocumentReference;

/**
 * Create a Bot Listener object by gathering the Listener metadata from a document.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Role
public interface WikiIRCBotListenerFactory
{
    /**
     * Searches the given document for a valid listener definition and for an active listener.
     *
     * @param documentReference name of the document to search for a listener definition.
     * @return true if the given document contains a listener class definition and at least one listener event class
     *         definition, false otherwise.
     */
    boolean containsWikiListener(DocumentReference documentReference);

    /**
     * Tries to build a {@link org.xwiki.ircbot.internal.wiki.WikiIRCBotListener} if a definition is found on the given document.
     *
     * @param documentReference name of the document on which the listener is defined.
     * @return a {@link org.xwiki.ircbot.internal.wiki.WikiIRCBotListener} corresponding to the listener definition found.
     * @throws IRCBotException if no listener definition is found or if an error is encountered while building
     *             the wiki listener.
     */
    WikiIRCBotListener createWikiListener(DocumentReference documentReference) throws IRCBotException;
}
