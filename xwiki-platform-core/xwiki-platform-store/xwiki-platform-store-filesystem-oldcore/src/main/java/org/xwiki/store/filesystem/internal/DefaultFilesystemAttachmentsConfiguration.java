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
package org.xwiki.store.filesystem.internal;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * All configuration options for Filesystem Attachment Store.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Singleton
public class DefaultFilesystemAttachmentsConfiguration implements FilesystemAttachmentsConfiguration
{
    /**
     * Prefix for configuration keys for the Filesystem Attachment Store.
     */
    private static final String OLDPREFIX = "store.fsattach.";

    private static final String PREFIX = "store.file.";

    /**
     * Configuration source from where to read configuration data from. This configuration is loaded when the
     * FilesystemAttachment Store is initialized, so we cannot use the default implementation for ConfigurationSource
     * because it looks at the main store which might be not initialized yet.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Override
    public boolean cleanOnStartup()
    {
        return this.configuration.getProperty(OLDPREFIX + "cleanOnStartup", Boolean.TRUE);
    }

    @Override
    public File getDirectory()
    {
        String directory = this.configuration.getProperty(PREFIX + "directory");

        return directory != null ? new File(directory) : null;
    }
}
