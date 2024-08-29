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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.AbstractSpacesConfigurationSource;

/**
 * The configuration source following the parent spaces of the current document to find an
 * {@link AttachmentMimetypeRestrictionClassDocumentInitializer#REFERENCE} XObject in a {@code WebPreferences}
 * document.
 *
 * @version $Id$
 * @since 14.10
 */
@Component
@Singleton
@Named(DefaultAttachmentMimetypeRestrictionSpacesConfigurationSource.HINT)
public class DefaultAttachmentMimetypeRestrictionSpacesConfigurationSource extends AbstractSpacesConfigurationSource
{
    /**
     * This class hint.
     */
    public static final String HINT = "attachment.mimetypeRestriction.configurationSource.spaces";

    @Inject
    @Named(DefaultAttachmentMimetypeRestrictionSpaceConfigurationSource.HINT)
    private ConfigurationSource spaceConfigurationSource;

    @Override
    protected ConfigurationSource getSpaceConfigurationSource()
    {
        return this.spaceConfigurationSource;
    }
}
