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

import org.xwiki.component.annotation.Role;

/**
 * Responsible for registering wiki macros against the ComponentManager at XE startup.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Role
public interface WikiMacroInitializer
{
    /**
     * Installs or upgrades xwiki classes required for defining wiki macros.
     * 
     * @throws Exception if an error occurs while installing / upgrading classes.
     */
    void installOrUpgradeWikiMacroClasses() throws Exception;

    /**
     * Searches for all the wiki macro definitions on the system and registers them against the macro manager.
     * 
     * @throws Exception if xwiki classes required for defining wiki macros are missing or if an error occurs while
     *             searching for existing wiki macros.
     */
    void registerExistingWikiMacros() throws Exception;

    /**
     * Searches for all the wiki macro definitions in the specified wiki and registers them against the macro manager.
     * 
     * @param wiki the name of the wiki to register wiki macros for
     * @throws Exception if xwiki classes required for defining wiki macros are missing or if an error occurs while
     *             searching for existing wiki macros.
     */
    void registerExistingWikiMacros(String wiki) throws Exception;
}
