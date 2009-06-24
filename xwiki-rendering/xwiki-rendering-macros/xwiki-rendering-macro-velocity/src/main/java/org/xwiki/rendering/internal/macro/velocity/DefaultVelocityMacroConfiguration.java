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
package org.xwiki.rendering.internal.macro.velocity;

import java.util.Properties;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationManager;
import org.xwiki.configuration.ConfigurationSourceCollection;
import org.xwiki.rendering.macro.velocity.VelocityMacroConfiguration;

/**
 * All configuration options for the Velocity macro.
 * 
 * @version $Id$
 * @since 1.9
 */
@Component
public class DefaultVelocityMacroConfiguration implements Initializable, VelocityMacroConfiguration
{
    /**
     * Allows reading the rendering configuration from where it's defined.
     */
    @Requirement
    private ConfigurationManager configurationManager;

    /**
     * Defines from where to read the rendering configuration data.
     */
    @Requirement
    private ConfigurationSourceCollection sourceCollection;

    /**
     * @see #getFilter()
     */
    private String filter = "indent";

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // Default Velocity properties
        Properties defaultProperties = new Properties();
        defaultProperties.setProperty("resource.loader", "webapp");

        this.configurationManager.initializeConfiguration(this, this.sourceCollection.getConfigurationSources(),
            "macro.velocity");
    }

    /**
     * @param filter the hint of the {@link org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter} component to
     *            use to modify velocity content before or after script execution.
     */
    public void setFilter(String filter)
    {
        this.filter = filter;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.velocity.VelocityMacroConfiguration#getFilter()
     */
    public String getFilter()
    {
        return this.filter;
    }
}
