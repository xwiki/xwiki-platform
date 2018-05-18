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
package org.xwiki.extension.xar.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.extension.xar.XarExtensionConfiguration;

/**
 * Default implementation of {@link XarExtensionConfiguration}.
 * 
 * @version $Id$
 * @since 10.5RC1
 */
@Component
@Singleton
public class DefaultXarExtensionConfiguration implements XarExtensionConfiguration
{
    private static final String PREFIX = "extension.xar.";

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Override
    public DocumentProtection getDocumentProtection()
    {
        return this.configuration.getProperty(PREFIX + "protection", DocumentProtection.WARNING);
    }
}
