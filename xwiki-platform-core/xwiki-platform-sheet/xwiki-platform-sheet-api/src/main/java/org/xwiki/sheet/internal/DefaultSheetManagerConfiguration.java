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
package org.xwiki.sheet.internal;

import java.util.Properties;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.sheet.SheetManagerConfiguration;

/**
 * The default configuration.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Component
public class DefaultSheetManagerConfiguration implements SheetManagerConfiguration, Initializable
{
    /** Prefix for configuration keys for the sheet manager. */
    private static final String PREFIX = "sheet.";

    /**
     * Defines from where to read the sheet manager configuration.
     */
    @Inject
    private ConfigurationSource configuration;

    /**
     * The default class sheet binding.
     */
    private Properties defaultClassSheetBinding = new Properties();

    @Override
    public String getDefaultClassSheet()
    {
        return configuration.getProperty(PREFIX + "defaultClassSheet", "XWiki.ClassSheet");
    }

    @Override
    public Properties getDefaultClassSheetBinding()
    {
        Properties props = new Properties();
        props.putAll(defaultClassSheetBinding);
        props.putAll(configuration.getProperty(PREFIX + "defaultClassSheetBinding", Properties.class));
        return props;
    }

    @Override
    public void initialize() throws InitializationException
    {
        // As you can see, the following sheets don't follow the naming convention.
        defaultClassSheetBinding.setProperty("XWiki.XWikiPreferences", "XWiki.AdminSheet");
        defaultClassSheetBinding.setProperty("XWiki.XWikiUsers", "XWiki.XWikiUserSheet");
        defaultClassSheetBinding.setProperty("XWiki.XWikiGroups", "XWiki.XWikiGroupSheet");
    }
}
