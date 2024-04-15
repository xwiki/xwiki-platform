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
package org.xwiki.attachment.internal.configuration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.attachment.configuration.AttachmentConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Default implementation for attachment configuration.
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
@Component
@Singleton
public class DefaultAttachmentConfiguration implements AttachmentConfiguration
{
    private static final String ATTACHMENT_MIMETYPE_ENABLE_COMMENTS_PROPERTY = "attachment.upload.enableComments";

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xWikiPropertiesConfigurationSource;

    @Override
    public boolean isCommentsEnabled()
    {
        return this.xWikiPropertiesConfigurationSource.getProperty(ATTACHMENT_MIMETYPE_ENABLE_COMMENTS_PROPERTY, false);
    }
}
