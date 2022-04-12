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
package org.xwiki.image.style.internal.configuration.source;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.CompositeConfigurationSource;

/**
 * {@link ConfigurationSource} for the Image Style configuration. Reads the values from the configuration page.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Component
@Singleton
@Named(ImageStyleConfigurationSource.HINT)
public class ImageStyleConfigurationSource extends CompositeConfigurationSource
{
    /**
     * Hint of the component.
     */
    public static final String HINT = "image.style";

    @Inject
    @Named("image.style.wiki.current")
    private ConfigurationSource currentWkiImageStyleSource;

    @Inject
    @Named("image.style.spaces")
    private ConfigurationSource spacesImageStyleSource;

    @Override
    public Iterator<ConfigurationSource> iterator()
    {
        return List.of(this.spacesImageStyleSource, this.currentWkiImageStyleSource).iterator();
    }
}
