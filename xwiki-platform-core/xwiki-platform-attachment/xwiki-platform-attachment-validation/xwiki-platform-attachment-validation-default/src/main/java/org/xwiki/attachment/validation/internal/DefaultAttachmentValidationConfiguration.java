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
package org.xwiki.attachment.validation.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.attachment.validation.AttachmentValidationConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

import static org.xwiki.attachment.validation.internal.AttachmentMimetypeRestrictionClassDocumentInitializer.ALLOWED_MIMETYPES_FIELD;
import static org.xwiki.attachment.validation.internal.AttachmentMimetypeRestrictionClassDocumentInitializer.BLOCKED_MIMETYPES_FIELD;

/**
 * Default implementation of the attachment configuration. Looks for the configuration:
 * <ol>
 *     <li>In the current space {@code WebPreferences}</li>
 *     <li>In {@code XWiki.XWikiPreferences}</li>
 *     <li>In {@code xwiki.properties}</li>
 * </ol>
 *
 * @version $Id$
 * @since 14.10RC1
 */
@Component
@Singleton
public class DefaultAttachmentValidationConfiguration implements AttachmentValidationConfiguration
{
    private static final String ATTACHMENT_MIMETYPE_ALLOW_LIST_PROPERTY = "attachment.upload.allowList";

    private static final String ATTACHMENT_MIMETYPE_BLOCK_LIST_PROPERTY = "attachment.upload.blockList";

    @Inject
    @Named(DefaultAttachmentMimetypeRestrictionSpacesConfigurationSource.HINT)
    private ConfigurationSource attachmentConfigurationSource;

    @Inject
    @Named(DefaultAttachmentMimetypeRestrictionWikiConfigurationSource.HINT)
    private ConfigurationSource wikiConfigurationSource;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xWikiPropertiesConfigurationSource;

    @Override
    public List<String> getAllowedMimetypes()
    {
        return getPropertyWithFallback(ALLOWED_MIMETYPES_FIELD, ATTACHMENT_MIMETYPE_ALLOW_LIST_PROPERTY);
    }

    @Override
    public List<String> getBlockerMimetypes()
    {
        return getPropertyWithFallback(BLOCKED_MIMETYPES_FIELD, ATTACHMENT_MIMETYPE_BLOCK_LIST_PROPERTY);
    }

    private List<String> getPropertyWithFallback(String attachmentXObjectProperty, String xWikiPropertiesProperty)
    {
        List<String> allowedMimetypes;
        if (this.attachmentConfigurationSource.containsKey(attachmentXObjectProperty)) {
            allowedMimetypes = this.attachmentConfigurationSource.getProperty(attachmentXObjectProperty);
        } else if (this.wikiConfigurationSource.containsKey(attachmentXObjectProperty)) {
            allowedMimetypes = this.wikiConfigurationSource.getProperty(attachmentXObjectProperty);
        } else {
            allowedMimetypes = this.xWikiPropertiesConfigurationSource.getProperty(xWikiPropertiesProperty, List.of());
        }
        return allowedMimetypes;
    }
}
