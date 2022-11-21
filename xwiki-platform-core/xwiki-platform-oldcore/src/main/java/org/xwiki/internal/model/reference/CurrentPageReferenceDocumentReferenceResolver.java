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
package org.xwiki.internal.model.reference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.PageReference;

/**
 * Converts a {@link PageReference} into a {@link DocumentReference} by checking if the non-terminal document exists
 * and if so returns its reference; if not, then return the terminal document reference unless that reference isn't
 * valid (which is the case for a top level terminal document), in which case the non-terminal reference is returned.
 *
 * Examples:
 * <ul>
 *   <li>{@code A} -> {@code A.WebHome} (when document {@code A.WebHome} exists)</li>
 *   <li>{@code A} -> {@code A.WebHome} (when document {@code A.WebHome} doesn't exist)</li>
 *   <li>{@code A/B} -> {@code A.B.WebHome} (when document {@code A.B.WebHome} exists)</li>
 *   <li>{@code A/B} -> {@code A.B} (when document {@code A.B.WebHome} doesn't exist)</li>
 * </ul>
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Component
@Named("current")
@Singleton
public class CurrentPageReferenceDocumentReferenceResolver implements DocumentReferenceResolver<PageReference>
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> currentReferenceDocumentReferenceResolver;

    @Inject
    private DocumentAccessBridge dab;

    @Inject
    private Logger logger;

    @Override
    public DocumentReference resolve(PageReference reference, Object... parameters)
    {
        DocumentReference result;

        // Try first to see if the reference is pointing to an existing non-terminal page. The conversion to a
        // DocumentReference will convert into a non-terminal document reference.
        DocumentReference documentReference = this.currentReferenceDocumentReferenceResolver.resolve(reference);
        if (exists(documentReference)) {
            result = documentReference;
        } else if (documentReference.getParent().getParent().getType() == EntityType.SPACE) {
            result = new DocumentReference(documentReference.getParent().getName(),
                documentReference.getParent().getParent(), documentReference.getParameters());
            if (!exists(result)) {
                result = documentReference;
            }
        } else {
            result = documentReference;
        }

        return result;
    }

    private boolean exists(DocumentReference documentReference)
    {
        try {
            return this.dab.exists(documentReference);
        } catch (Exception e) {
            this.logger.error("Failed to check the existence of the document with reference [{}]", documentReference,
                e);
        }

        return false;
    }
}
