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
package org.xwiki.image.style.internal.configuration.source;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;

import static org.xwiki.image.style.internal.configuration.source.CurrentWikiImageStyleConfigurationSource.XCLASS_REFERENCE;

/**
 * Space image style configuration, check for the presence of an {@link CurrentWikiImageStyleConfigurationSource#XCLASS_REFERENCE}
 * XObject in the {@code WebPreferences} document of this space.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Component
@Singleton
@Named("image.style.space")
public class SpaceImageStyleConfigurationSource extends AbstractXClassConfigurationSource
{
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    protected String getCacheId()
    {
        return "configuration.image.style.space";
    }

    @Override
    protected String getCacheKeyPrefix()
    {
        DocumentReference currentDocumentReference = this.documentAccessBridge.getCurrentDocumentReference();
        if (currentDocumentReference != null) {
            return this.referenceSerializer.serialize(currentDocumentReference.getParent());
        }

        return null;
    }

    @Override
    protected DocumentReference getDocumentReference()
    {
        return this.documentReferenceResolver.resolve("WebPreferences");
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return XCLASS_REFERENCE;
    }
}
