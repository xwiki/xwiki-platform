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
package org.xwiki.rendering.macro.wikibridge;

import org.xwiki.bridge.DocumentName;
import org.xwiki.component.annotation.ComponentRole;

/**
 * Component interface responsible for managing wiki macro instances.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@ComponentRole
public interface WikiMacroManager
{
    /**
     * Registers the given {@link WikiMacro} against the ComponentManager matching the Wiki Macro visibility defined
     * (Current User, Current Wiki, Global). For example Macros defined with a "Current User" visibility are
     * registered against the User Component Manager so that they are only visible from that Component Manager and not
     * from other Component Manager.
     * <p/>
     * Note that the Execution Context must be set properly (the current user or the current wiki must be set) prior
     * to calling this API.
     *
     * @param documentName the name of the document which contains the wiki macro
     * @param wikiMacro the {@link org.xwiki.rendering.macro.wikibridge.WikiMacro} instance
     * @exception WikiMacroException if a problem happened when registering the macro (document doesn't exist,
     *            not enough privilege, etc)
     * @since 2.2.M1
     */
    void registerWikiMacro(DocumentName documentName, WikiMacro wikiMacro) throws WikiMacroException;

    /**
     * Unregisters the wiki macro defined on the given document (if there is one).
     * 
     * @param documentName the name of the document which contains the wiki macro
     * @exception WikiMacroException if a problem happened when registering the macro (document doesn't exist,
     *            not enough privilege, etc)
     * @since 2.2.M1
     */
    void unregisterWikiMacro(DocumentName documentName) throws WikiMacroException;

    /**
     * Utility method for querying {@link WikiMacroManager} to see if there is a {@link WikiMacro} already registered
     * for the given document.
     * 
     * @param documentName the name of the document which contains the wiki macro
     * @return true if there is already a macro registered under the given document name
     * @since 2.2.M1
     */
    boolean hasWikiMacro(DocumentName documentName);
}
