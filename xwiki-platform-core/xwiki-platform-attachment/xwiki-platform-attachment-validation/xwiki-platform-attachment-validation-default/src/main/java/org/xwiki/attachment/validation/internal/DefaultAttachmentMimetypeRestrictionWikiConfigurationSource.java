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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;

import static com.xpn.xwiki.internal.mandatory.XWikiPreferencesDocumentInitializer.NAME;
import static org.xwiki.component.wiki.internal.WikiComponentConstants.CLASS_SPACE_NAME;

/**
 * Look for the attachment mimetype restriction configuration in a space.
 *
 * @version $Id$
 * @since 14.10
 */
@Component
@Singleton
@Named(DefaultAttachmentMimetypeRestrictionWikiConfigurationSource.HINT)
public class DefaultAttachmentMimetypeRestrictionWikiConfigurationSource extends AbstractDocumentConfigurationSource
{
    /**
     * Hint of this configuration source.
     */
    public static final String HINT = "attachment.mimetypeRestriction.wiki";

    @Override
    protected DocumentReference getDocumentReference()
    {
        return new DocumentReference(NAME, new SpaceReference(CLASS_SPACE_NAME, getCurrentWikiReference()));
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return AttachmentMimetypeRestrictionClassDocumentInitializer.REFERENCE;
    }

    @Override
    protected String getCacheId()
    {
        return HINT;
    }
}
