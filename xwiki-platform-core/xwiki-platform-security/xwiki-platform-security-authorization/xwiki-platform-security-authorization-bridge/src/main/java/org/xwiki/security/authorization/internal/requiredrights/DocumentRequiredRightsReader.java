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
package org.xwiki.security.authorization.internal.requiredrights;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Read the required rights from a document.
 *
 * @version $Id$
 * @since 16.6.0RC1
 */
@Component(roles = DocumentRequiredRightsReader.class)
@Singleton
public class DocumentRequiredRightsReader
{
    private static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference("XWiki", "RequiredRightClass");

    private static final String PROPERTY_NAME = "level";

    /**
     * Read the required rights from a document.
     *
     * @param document the document to read the required rights from
     * @return the required rights
     */
    public DocumentRequiredRights readRequiredRights(XWikiDocument document)
    {
        return new DocumentRequiredRights(document.isEnforceRequiredRights(),
            document.getXObjects(CLASS_REFERENCE).stream()
                .filter(Objects::nonNull)
                .map(this::readRequiredRight)
                // Don't allow edit right/edit right implies no extra right.
                .filter(requiredRight -> !Right.EDIT.equals(requiredRight.right()))
                .collect(Collectors.toUnmodifiableSet()));
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
                entityType = EntityType.valueOf(levelRight[0].toUpperCase());
            }
        }

        // TODO: add some handling for invalid values.
        Set<EntityType> targetedEntityTypes = right.getTargetedEntityType();
        if (targetedEntityTypes == null) {
            // This means the right targets only the farm level, which is null.
            entityType = null;
        } else {
            EntityReference entityReference = object.getDocumentReference().extractReference(entityType);
            // Try to get the highest level where this right can be assigned. This is done to ensure that, e.g.,
            // programming right can imply admin right on the wiki level even if programming right is only specified on
            // the document level.
            while (!targetedEntityTypes.contains(entityType) && entityReference != null) {
                entityType = entityReference.getType();
                entityReference = entityReference.getParent();
            }
        }

        return new DocumentRequiredRight(right, entityType);
    }
}
