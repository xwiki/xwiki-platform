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
package org.xwiki.rendering.internal.macro.script;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.rendering.macro.script.ScriptMacroConfiguration;

/**
 * All configuration options for the Script macro.
 * 
 * @version $Id$
 * @since 2.4M2
 */
@Component
public class DefaultScriptMacroConfiguration implements ScriptMacroConfiguration
{
    /** Prefix for configuration keys for the Script Macro module. */
    private static final String PREFIX = "rendering.macro.script.";

    /** @see #isNestedScriptsEnabled() */
    private static final Boolean DEFAULT_NESTEDSCRIPTS = Boolean.FALSE;

    /** Defines from where to read the rendering configuration data. */
    @Requirement
    private ConfigurationSource configuration;

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.macro.script.ScriptMacroConfiguration#isNestedScriptsEnabled()
     */
    public boolean isNestedScriptsEnabled()
    {
        return configuration.getProperty(PREFIX + "nestedscripts.enabled", DEFAULT_NESTEDSCRIPTS).booleanValue();
    }
}
