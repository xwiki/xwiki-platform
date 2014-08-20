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

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

import javax.inject.Inject;
import javax.inject.Singleton;

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
    private static final String PREFIX = "store.fsattach.";

    /**
     * Configuration source from where to read configuration data from.
     */
    @Inject
    private ConfigurationSource configuration;

    @Override
    public boolean cleanOnStartup()
    {
        return this.configuration.getProperty(PREFIX + "cleanOnStartup", Boolean.TRUE);
    }
}
