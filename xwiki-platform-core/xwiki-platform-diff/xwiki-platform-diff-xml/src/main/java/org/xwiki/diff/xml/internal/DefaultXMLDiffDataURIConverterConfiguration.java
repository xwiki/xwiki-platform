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
package org.xwiki.diff.xml.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.diff.xml.XMLDiffDataURIConverterConfiguration;

/**
 * Configuration for the XML diff.
 *
 * @version $Id$
 * @since 14.10.15
 * @since 15.5.1
 * @since 15.6
 */
@Component
@Singleton
public class DefaultXMLDiffDataURIConverterConfiguration implements XMLDiffDataURIConverterConfiguration
{
    private static final String PREFIX = "diff.xml.dataURI";

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @Override
    public int getHTTPTimeout()
    {
        return this.configurationSource.getProperty(getFullKeyName("httpTimeout"), Integer.class, 10);
    }

    @Override
    public long getMaximumContentSize()
    {
        return this.configurationSource.getProperty(getFullKeyName("maximumContentSize"), Long.class, 1024L * 1024L);
    }

    @Override
    public boolean isEnabled()
    {
        return this.configurationSource.getProperty(getFullKeyName("enabled"), Boolean.class, true);
    }

    private String getFullKeyName(String shortKeyName)
    {
        return String.format("%s.%s", PREFIX, shortKeyName);
    }

}
