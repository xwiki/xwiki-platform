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
package org.xwiki.platform.security.requiredrights.rest.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.internal.document.DocumentRequiredRightsReader;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Object;

import static org.xwiki.internal.document.DocumentRequiredRightsReader.PROPERTY_NAME;

/**
 * Helper component to update the required rights of a document based on a REST object.
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
@Component(roles = DocumentRequiredRightsUpdater.class)
@Singleton
public class DocumentRequiredRightsUpdater
{
    @Inject
    private DocumentRequiredRightsReader documentRequiredRightsReader;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private ModelContext modelContext;

    /**
     * Updates the required rights configuration of a document based on the specified
     * {@code documentRequiredRights} object. The method ensures that the enforcement status
     * and any associated rights objects are correctly synchronized with the target document.
     * If changes are made, the document is saved with an appropriate summary.
     *
     * @param documentRequiredRights an object representing the required rights configuration
     *                                to be applied to the document, including enforcement status
     *                                and a set of rights
     * @param doc the document to which the required rights configuration is applied
     * @throws XWikiException if an error occurs while updating the document or saving changes
     */
    public void updateRequiredRights(
        org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRequiredRights documentRequiredRights, Document doc)
        throws XWikiException
    {
        EntityReference currentEntityReference = this.modelContext.getCurrentEntityReference();

        try {
            this.modelContext.setCurrentEntityReference(doc.getDocumentReference());

            boolean modified = false;

            if (documentRequiredRights.isEnforce() != doc.isEnforceRequiredRights()) {
                doc.setEnforceRequiredRights(documentRequiredRights.isEnforce());
                modified = true;
            }

            if (documentRequiredRights.isEnforce()) {
                modified |= updateRequiredRightObjects(documentRequiredRights, doc);
            }

            if (modified) {
                doc.save(this.localizationManager.getTranslationPlain("security.requiredrights.rest.saveSummary"));
            }
        } finally {
            this.modelContext.setCurrentEntityReference(currentEntityReference);
        }
    }

    private boolean updateRequiredRightObjects(
        org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRequiredRights documentRequiredRights, Document doc)
        throws XWikiException
    {
        boolean modified = false;

        Set<String> formattedRights = buildFormattedRights(documentRequiredRights, doc.getDocumentReference());

        List<com.xpn.xwiki.api.Object> existingObjects = doc.getObjects(DocumentRequiredRightsReader.CLASS_REFERENCE);

        Set<String> alreadyExistingRights = new HashSet<>();

        List<com.xpn.xwiki.api.Object> objectsToRemove = new ArrayList<>();

        for (com.xpn.xwiki.api.Object object : existingObjects) {
            String existingValue = Objects.toString(object.getValue(PROPERTY_NAME), "");
            if (formattedRights.contains(existingValue)) {
                alreadyExistingRights.add(existingValue);
            } else {
                objectsToRemove.add(object);
            }
        }

        // Add the missing rights.
        for (String formattedRight : formattedRights) {
            if (!alreadyExistingRights.contains(formattedRight)) {
                com.xpn.xwiki.api.Object object;
                // Reuse an existing object if possible.
                if (objectsToRemove.isEmpty()) {
                    object = doc.newObject(DocumentRequiredRightsReader.CLASS_REFERENCE);
                } else {
                    object = objectsToRemove.remove(objectsToRemove.size() - 1);
                }
                object.set(PROPERTY_NAME, formattedRight);
                modified = true;
            }
        }

        // Remove the extra objects.
        for (Object object : objectsToRemove) {
            doc.removeObject(object);
            modified = true;
        }

        return modified;
    }

    private Set<String> buildFormattedRights(
        org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRequiredRights documentRequiredRights,
        DocumentReference baseDocumentReference)
    {
        return documentRequiredRights.getRights().stream()
            .map(requiredRight -> {
                Right right = Right.toRight(requiredRight.getRight());

                if (right.equals(Right.ILLEGAL)) {
                    throw new WebApplicationException("Illegal right " + requiredRight.getRight(),
                        Response.Status.BAD_REQUEST);
                }

                EntityType entityType = resolveEntityType(requiredRight.getScope());

                // Check if we can omit the entity type, i.e., if resolving the entity type from "DOCUMENT" is the
                // same as resolving it from the specified value.
                if (entityType != EntityType.DOCUMENT) {
                    EntityType resolvedEntityType =
                        this.documentRequiredRightsReader.getEffectiveEntityType(right, entityType,
                            baseDocumentReference);

                    EntityType resolvedEmptyEntityType =
                        this.documentRequiredRightsReader.getEffectiveEntityType(right, EntityType.DOCUMENT,
                            baseDocumentReference);
                    if (resolvedEntityType == resolvedEmptyEntityType) {
                        entityType = EntityType.DOCUMENT;
                    }
                }

                // We currently don't support "null" entity type except for rights that are only allowed at the farm
                // level.
                if (entityType == null) {
                    throw new WebApplicationException(
                        "Invalid scope %s for right %s".formatted(requiredRight.getScope(), requiredRight.getRight()),
                        Response.Status.BAD_REQUEST);
                }

                return (entityType == EntityType.DOCUMENT ? "" : entityType.getLowerCase() + "_")
                    + right.getName();
            })
            // Store as LinkedHashSet to preserve order.
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static EntityType resolveEntityType(String scope)
    {
        EntityType entityType;
        if (scope == null || "null".equals(scope)) {
            entityType = null;
        } else if (StringUtils.isNotBlank(scope)) {
            entityType = EntityType.valueOf(scope.toUpperCase());
        } else {
            entityType = EntityType.DOCUMENT;
        }
        return entityType;
    }
}
