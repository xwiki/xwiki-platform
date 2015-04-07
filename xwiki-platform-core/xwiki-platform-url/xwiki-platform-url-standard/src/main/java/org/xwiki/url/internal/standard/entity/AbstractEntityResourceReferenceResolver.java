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
package org.xwiki.url.internal.standard.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.url.ExtendedURL;

/**
 * Common code for Entity Resource Reference Resolvers.
 *
 * @version $Id$
 * @since 6.3M1
 */
public abstract class AbstractEntityResourceReferenceResolver implements ResourceReferenceResolver<ExtendedURL>
{
    /**
     * Used to resolve blanks in entity references when the URL doesn't specify all parts of an entity reference.
     */
    private EntityReferenceResolver<EntityReference> defaultReferenceEntityReferenceResolver;

    protected abstract WikiReference extractWikiReference(ExtendedURL url);

    @Override
    public ResourceReference resolve(ExtendedURL url, Map<String, Object> parameters)
        throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        EntityResourceReference entityURL;

        // Extract the wiki reference from the URL
        WikiReference wikiReference = extractWikiReference(url);

        // Rules based on counting the url segments:
        // - 0 segments (e.g. ""): default document reference, "view" action
        // - 1 segment (e.g. "/", "/Document"): default space, specified document (and default if empty), "view" action
        // - 2 segments (e.g. "/Space/", "/Space/Document"): specified space, document (and default doc if empty),
        //   "view" action
        // - 3 segments (e.g. "/action/Space/Document"): specified space, document (and default doc if empty),
        //   specified action
        // - 4 segments (e.g. "/download/Space/Document/attachment"): specified space, document and attachment (and
        //   default doc if empty), "download" action
        // - 4 segments or more (e.g. "/action/Space/Document/whatever/else"): specified space, document (and default
        //     doc if empty), specified "action" (if action != "download"), trailing segments ignored

        List<String> pathSegments = url.getSegments();
        String spaceName = null;
        String pageName = null;
        String attachmentName = null;
        String action = "view";

        if (pathSegments.size() == 1) {
            pageName = pathSegments.get(0);
        } else if (pathSegments.size() == 2) {
            spaceName = pathSegments.get(0);
            pageName = pathSegments.get(1);
        } else if (pathSegments.size() >= 3) {
            action = pathSegments.get(0);
            spaceName = pathSegments.get(1);
            pageName = pathSegments.get(2);
            if (action.equals("download") && pathSegments.size() >= 4) {
                attachmentName = pathSegments.get(3);
            }
        }

        entityURL = new EntityResourceReference(
            buildEntityReference(wikiReference, spaceName, pageName, attachmentName),
            EntityResourceAction.fromString(action));

        copyParameters(url, entityURL);

        return entityURL;
    }

    /**
     * Normalize the extracted space/page to resolve empty/null values and replace them with default values.
     *
     * @param wikiReference the wiki reference as extracted from the URL
     * @param spaceName the space name as extracted from the URL (can be empty or null)
     * @param pageName the page name as extracted from the URL (can be empty or null)
     * @param attachmentName the attachment name as extracted from the URL (can be empty or null)
     * @return the absolute Entity Reference
     */
    private EntityReference buildEntityReference(WikiReference wikiReference, String spaceName, String pageName,
        String attachmentName)
    {
        EntityReference reference = wikiReference;
        EntityType entityType = EntityType.DOCUMENT;
        if (!StringUtils.isEmpty(spaceName)) {
            reference = new EntityReference(spaceName, EntityType.SPACE, reference);
        }
        if (!StringUtils.isEmpty(pageName)) {
            reference = new EntityReference(pageName, EntityType.DOCUMENT, reference);
        }
        if (!StringUtils.isEmpty(attachmentName)) {
            reference = new EntityReference(attachmentName, EntityType.ATTACHMENT, reference);
            entityType = EntityType.ATTACHMENT;
        }
        return this.defaultReferenceEntityReferenceResolver.resolve(reference, entityType);
    }

    /**
     * Copies query string parameters from the passed {@link org.xwiki.url.ExtendedURL} to the passed {@link org.xwiki.resource.entity.EntityResourceReference}.
     *
     * @param source the source URL from where to get the query string parameters
     * @param target the {@link org.xwiki.resource.entity.EntityResourceReference} on which to copy the query string parameters
     */
    private void copyParameters(ExtendedURL source, EntityResourceReference target)
    {
        // Add the Query string parameters from the passed URL in the returned XWikiURL
        if (source.getURI().getQuery() != null) {
            for (String nameValue : Arrays.asList(source.getURI().getQuery().split("&"))) {
                String[] pair = nameValue.split("=", 2);
                // Check if the parameter has a value or not.
                if (pair.length == 2) {
                    target.addParameter(pair[0], pair[1]);
                } else {
                    target.addParameter(pair[0], null);
                }
            }
        }
    }
}
