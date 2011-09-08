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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.rendering.transformation.Transformation;

/**
 * All configuration options for the Rendering subsystem.
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Component
@Singleton
public class DefaultXWikiRenderingConfiguration implements XWikiRenderingConfiguration
{
    /**
     * Prefix for configuration keys for the Rendering module.
     */
    private static final String PREFIX = "rendering.";

    /**
     * @see #getLinkLabelFormat()
     */
    private static final String DEFAULT_LINK_LABEL_FORMAT = "%p";

    /**
     * Defines from where to read the rendering configuration data.
     */
    @Inject
    private ConfigurationSource configuration;

    /**
     * Used to convert transformation component hints into {@link org.xwiki.rendering.transformation.Transformation}
     * objects.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public String getLinkLabelFormat()
    {
        return this.configuration.getProperty(PREFIX + "linkLabelFormat", DEFAULT_LINK_LABEL_FORMAT);
    }

    @Override
    public int getImageWidthLimit()
    {
        return this.configuration.getProperty(PREFIX + "imageWidthLimit", -1);
    }

    @Override
    public int getImageHeightLimit()
    {
        return this.configuration.getProperty(PREFIX + "imageHeightLimit", -1);
    }

    @Override
    public boolean isImageDimensionsIncludedInImageURL()
    {
        return this.configuration.getProperty(PREFIX + "imageDimensionsIncludedInImageURL", true);
    }

    @Override
    public Properties getInterWikiDefinitions()
    {
        return this.configuration.getProperty(PREFIX + "interWikiDefinitions", Properties.class);
    }

    /**
     * {@inheritDoc}
     * @since 2.6RC1
     */
    @Override
    public List<Transformation> getTransformations()
    {
        List<Transformation> transformations = new ArrayList<Transformation>();
        for (String hint : this.configuration.getProperty(PREFIX + "transformations",
            Arrays.asList("macro", "icon")))
        {
            try {
                transformations.add(this.componentManager.lookup(Transformation.class, hint));
            } catch (ComponentLookupException e) {
                this.logger.warn("Failed to locate transformation with hint [" + hint + "], ignoring it.");
            }
        }
        Collections.sort(transformations);
        return transformations;
    }
}
