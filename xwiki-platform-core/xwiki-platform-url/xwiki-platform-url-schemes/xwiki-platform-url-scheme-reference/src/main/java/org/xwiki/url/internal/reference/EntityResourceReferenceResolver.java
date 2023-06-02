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
package org.xwiki.url.internal.reference;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.AbstractResourceReferenceResolver;

/**
 * Parses an {@link ExtendedURL} written in the "reference" format and generates a
 * {@link org.xwiki.resource.ResourceReference} out of it. The format is the following:
 * <ul>
 *   <li>UC1: {@code <action>/<entity reference type>/<entity reference>}</li>
 *   <li>UC2: {@code <action>/<entity reference>} ==> type = page</li>
 *   <li>UC3: {@code <entity reference>} ==> type = page, action = view</li>
 * </ul>
 * Examples (with XWiki deployed in the ROOT context):
 * <ul>
 *   <li>http://localhost:8080/entity/view/page/wiki:space.page</li>
 *   <li>http://localhost:8080/entity/export/attach/wiki:space.page@image.png</li>
 *   <li>http://localhost:8080/entity/export/attach/wiki:space.page@image.png?format=xar|pdf|html</li>
 *   <li>http://localhost:8080/entity/view/wiki:space.page</li>
 *   <li>http://localhost:8080/entity/wiki:space.page</li>
 *   <li>http://localhost:8080/wiki:space.page</li>
 * </ul>
 *
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Named("reference/entity")
@Singleton
public class EntityResourceReferenceResolver extends AbstractResourceReferenceResolver
{
    private static final String ACTION_VIEW = "view";
    private static final String ACTION_EXPORT = "export";

    @Inject
    private EntityReferenceResolver<String> defaultEntityReferenceResolver;

    @Override
    public EntityResourceReference resolve(ExtendedURL extendedURL, ResourceType resourceType,
        Map<String, Object> parameters) throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        EntityResourceReference entityURL;

        // UC1: <action>/<entity reference type>/<entity reference>
        // UC2: <action>/<entity reference> ==> type = page
        // UC3: <entity reference> ==> type = page, action = view
        List<String> pathSegments = extendedURL.getSegments();
        String action = ACTION_VIEW;
        EntityType entityType;
        String entityReferenceAsString;

        if (pathSegments.size() == 3) {
            action = pathSegments.get(0);
            entityType = computeEntityType(pathSegments.get(1));
            entityReferenceAsString = pathSegments.get(2);
        } else if (pathSegments.size() == 2) {
            action = pathSegments.get(0);
            entityType = computeDefaultEntityType(action);
            entityReferenceAsString = pathSegments.get(1);
        } else if (pathSegments.size() == 1) {
            entityType = computeDefaultEntityType(action);
            entityReferenceAsString = pathSegments.get(0);
        } else {
            throw new CreateResourceReferenceException(String.format("Invalid Entity URL [%s]",
                extendedURL.serialize()));
        }

        // Convert the string representation of the Entity reference into a proper EntityResourceReference.
        entityURL = new EntityResourceReference(
            this.defaultEntityReferenceResolver.resolve(entityReferenceAsString, entityType),
            EntityResourceAction.fromString(action), extendedURL.getURI().getFragment());

        return entityURL;
    }

    private EntityType computeEntityType(String entityType) throws CreateResourceReferenceException
    {
        EntityType type;
        if (entityType.equals("page")) {
            type = EntityType.DOCUMENT;
        } else if (entityType.equals("attach")) {
            type = EntityType.ATTACHMENT;
        } else {
            throw new CreateResourceReferenceException(String.format("Unknown Entity type [%s]", entityType));
        }
        return type;
    }

    private EntityType computeDefaultEntityType(String action) throws CreateResourceReferenceException
    {
        EntityType type;
        if (action.equals(ACTION_VIEW)) {
            type = EntityType.DOCUMENT;
        } else if (action.equals(ACTION_EXPORT)) {
            type = EntityType.ATTACHMENT;
        } else {
            throw new CreateResourceReferenceException(String.format("Unknown Entity type for [%s]", action));
        }

        return type;
    }
}
