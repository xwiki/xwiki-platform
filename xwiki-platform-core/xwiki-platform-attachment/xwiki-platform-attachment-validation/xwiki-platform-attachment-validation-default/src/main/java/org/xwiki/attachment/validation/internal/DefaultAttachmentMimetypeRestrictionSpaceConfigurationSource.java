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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;

/**
 * This configuration source search for a {@link AttachmentMimetypeRestrictionClassDocumentInitializer#REFERENCE}
 * XObject in a {@code WebPreferences} document aside the current document.
 *
 * @version $Id$
 * @since 14.10
 */
@Component
@Singleton
@Named(DefaultAttachmentMimetypeRestrictionSpaceConfigurationSource.HINT)
public class DefaultAttachmentMimetypeRestrictionSpaceConfigurationSource extends AbstractDocumentConfigurationSource
{
    /**
     * Hint of this configuration source.
     */
    public static final String HINT = "attachment.mimetypeRestriction.space";

    private static final String SPACE_PREFERENCES = "WebPreferences";

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    protected DocumentReference getDocumentReference()
    {
        // Get the current document reference to extract the wiki and space names.
        DocumentReference currentDocumentReference = this.documentAccessBridge.getCurrentDocumentReference();

        if (currentDocumentReference != null) {
            // Add the current spaces and current wiki references to the Web Preferences document reference to form
            // an absolute reference.
            return new DocumentReference(SPACE_PREFERENCES, (SpaceReference) currentDocumentReference.getParent());
        }

        return null;
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
