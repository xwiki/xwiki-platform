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
package org.xwiki.ircbot;

import java.util.List;
import java.util.regex.Pattern;

import org.xwiki.component.annotation.Role;

/**
 * Configuration for the Document Modifier Event Listener (ie the listener that sends IRC messages when a document is
 * modified).
 *
 * @version $Id$
 * @since 4.0M2
 */
@Role
public interface DocumentModifiedEventListenerConfiguration
{
    /**
     * @return the regex patterns to use to decide if a notification for a given document reference should be sent or
     *         not. Note that the pattern is to be done on the full serialized document reference.
     * @throws IRCBotException if an error happens getting the exclusion patterns
     */
    List<Pattern> getExclusionPatterns() throws IRCBotException;
}
