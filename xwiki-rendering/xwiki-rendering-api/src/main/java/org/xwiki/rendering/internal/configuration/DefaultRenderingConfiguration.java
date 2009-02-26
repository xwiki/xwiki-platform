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
package org.xwiki.rendering.internal.configuration;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationManager;
import org.xwiki.configuration.ConfigurationSourceCollection;
import org.xwiki.rendering.configuration.RenderingConfiguration;

/**
 * @version $Id: $
 * @since 1.6M1
 */
public class DefaultRenderingConfiguration implements Initializable, RenderingConfiguration
{
    /**
     * @see org.xwiki.rendering.configuration.RenderingConfiguration#getLinkLabelFormat()
     */
    private String linkLabelFormat = "%p";

    /**
     * Injected by the Component Manager.
     */
    private ConfigurationManager configurationManager;

    /**
     * Injected by the Component Manager.
     */
    private ConfigurationSourceCollection sourceCollection;

    /**
     * {@inheritDoc}
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.configurationManager.initializeConfiguration(this, this.sourceCollection.getConfigurationSources(),
            "rendering");
    }

    /**
     * @see org.xwiki.rendering.configuration.RenderingConfiguration#getLinkLabelFormat()
     */
    public String getLinkLabelFormat()
    {
        return this.linkLabelFormat;
    }

    public void setLinkLabelFormat(String linkLabelFormat)
    {
        this.linkLabelFormat = linkLabelFormat;
    }
}
