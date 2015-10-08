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
package com.xpn.xwiki.internal.model.reference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Specialized version of {@link org.xwiki.model.reference.EntityReferenceResolver} which can be considered a helper
 * component to resolve {@link org.xwiki.model.reference.DocumentReference} objects from Entity Reference (when they
 * miss some parent references or have NULL values).
 * <p>
 * The goal is to have the document that correspond the best to the passed reference (if a space then get the space home
 * page, if a wiki then get the wiki home page, if an attachment then get the document where it's located, etc).
 *
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named("currentgetdocument")
@Singleton
public class CurrentGetDocumentReferenceDocumentReferenceResolver implements DocumentReferenceResolver<EntityReference>
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> currentDocumentResolver;

    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    @Inject
    private DocumentReferenceResolver<EntityReference> defaultResolver;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public DocumentReference resolve(EntityReference initialReference, Object... parameters)
    {
        if (initialReference.getType().ordinal() < EntityType.DOCUMENT.ordinal()) {
            if (EntityType.WIKI.equals(initialReference.getType())) {
                // Use whatever document reference that is configured as wiki homepage.
                String wikiId = initialReference.getName();
                try {
                    WikiDescriptor wikiDescriptor = wikiDescriptorManager.getById(wikiId);
                    if (wikiDescriptor != null) {
                        DocumentReference wikiHomepageReference = wikiDescriptor.getMainPageReference();
                        return wikiHomepageReference;
                    }
                } catch (Exception e) {
                    // It would not be safe to do any assumptions a this level since we risk affecting the wrong
                    // document.
                    throw new RuntimeException(String.format(
                        "Failed to get wiki descriptor while resolving reference [%s]", initialReference), e);
                }
            }

            // Get the complete initial reference
            EntityReference currentParentReference =
                this.currentResolver.resolve(initialReference, initialReference.getType(), parameters);

            // Resolve missing lower part as default (to get space home page and wiki home page)
            return this.defaultResolver.resolve(currentParentReference, parameters);
        } else {
            return this.currentDocumentResolver.resolve(initialReference, parameters);
        }
    }
}
