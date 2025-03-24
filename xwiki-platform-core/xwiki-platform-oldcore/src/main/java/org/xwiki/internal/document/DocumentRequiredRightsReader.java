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
package org.xwiki.internal.document;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Read the required rights from a document.
 *
 * @version $Id$
 * @since 16.10.0RC1
 */
@Component(roles = DocumentRequiredRightsReader.class)
@Singleton
public class DocumentRequiredRightsReader
{
    /**
     * The (local) class reference of the required right class.
     */
    public static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference("XWiki", "RequiredRightClass");

    /**
     * The name of the property that stores the value.
     */
    public static final String PROPERTY_NAME = "level";

    private static final DocumentRequiredRights ENFORCED_EMPTY = new DocumentRequiredRights(true, Set.of());

    private static final DocumentRequiredRights ENFORCED_SCRIPT = new DocumentRequiredRights(true,
        Set.of(new DocumentRequiredRight(Right.SCRIPT, EntityType.DOCUMENT)));

    private static final DocumentRequiredRights ENFORCED_PROGRAMMING = new DocumentRequiredRights(true,
        Set.of(new DocumentRequiredRight(Right.PROGRAM, null)));

    private static final DocumentRequiredRights ENFORCED_ADMIN = new DocumentRequiredRights(true,
        Set.of(new DocumentRequiredRight(Right.ADMIN, EntityType.WIKI)));

    private static final List<DocumentRequiredRights> STATIC_INSTANCES = List.of(ENFORCED_EMPTY, ENFORCED_SCRIPT,
        ENFORCED_PROGRAMMING, ENFORCED_ADMIN);

    @Inject
    private Logger logger;

    /**
     * Read the required rights from a document.
     *
     * @param document the document to read the required rights from
     * @return the required rights
     */
    public DocumentRequiredRights readRequiredRights(XWikiDocument document)
    {
        boolean enforce = document.isEnforceRequiredRights();
        Set<DocumentRequiredRight> rights = document.getXObjects(CLASS_REFERENCE).stream()
            .filter(Objects::nonNull)
            .map(this::readRequiredRight)
            // Don't allow edit right/edit right implies no extra right.
            // Filter out invalid values.
            .filter(requiredRight ->
                !Right.EDIT.equals(requiredRight.right()) && !Right.ILLEGAL.equals(requiredRight.right()))
            .collect(Collectors.toUnmodifiableSet());

        // Try returning a static instance to avoid creating lots of objects that contain the same values as most
        // documents will be in the case of one of the static instances. This is also to reduce the memory usage of
        // the cache.
        if (!enforce && rights.isEmpty()) {
            return DocumentRequiredRights.EMPTY;
        }

        // Return the static instance that has the same set of rights.
        if (enforce) {
            for (DocumentRequiredRights staticInstance : STATIC_INSTANCES) {
                if (staticInstance.rights().equals(rights)) {
                    return staticInstance;
                }
            }
        }

        return new DocumentRequiredRights(enforce, rights);
    }

    private DocumentRequiredRight readRequiredRight(BaseObject object)
    {
        String value = object.getStringValue(PROPERTY_NAME);
        EntityType entityType = EntityType.DOCUMENT;
        Right right = Right.toRight(value);
        if (right.equals(Right.ILLEGAL)) {
            String[] levelRight = StringUtils.split(value, "_", 2);
            if (levelRight.length == 2) {
                right = Right.toRight(levelRight[1]);
                try {
                    entityType = EntityType.valueOf(levelRight[0].toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Ensure that we return an illegal right even if the right part of the value could be parsed.
                    right = Right.ILLEGAL;
                    this.logger.warn("Illegal required right value [{}] in object [{}]", value, object.getReference());
                }
            }
        }

        Set<EntityType> targetedEntityTypes = right.getTargetedEntityType();
        if (targetedEntityTypes == null) {
            // This means the right targets only the farm level, which is null.
            entityType = null;
        } else {
            EntityReference entityReference = object.getDocumentReference().extractReference(entityType);
            // The specified entity type seems to be below the document level. Fall back to document level instead.
            if (entityReference == null) {
                entityReference = object.getDocumentReference();
            }
            // Try to get the lowest level where this right can be assigned. This is done to ensure that, e.g.,
            // programming right can imply admin right on the wiki level even if programming right is only specified on
            // the document level.
            while (!targetedEntityTypes.contains(entityType) && entityReference != null) {
                entityType = entityReference.getType();
                entityReference = entityReference.getParent();
            }

            // If we couldn't find a targeted entity type, revert to the document level.
            if (!targetedEntityTypes.contains(entityType)) {
                entityType = EntityType.DOCUMENT;
            }
        }

        return new DocumentRequiredRight(right, entityType);
    }
}
