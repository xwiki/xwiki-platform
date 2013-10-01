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
package org.xwiki.url.internal.standard;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.EntityResource;
import org.xwiki.resource.ResourceCreationException;
import org.xwiki.resource.ResourceFactory;
import org.xwiki.resource.UnsupportedResourceException;
import org.xwiki.url.internal.ExtendedURL;

/**
 * Factory that generates {@link EntityResource} out of {@link ExtendedURL} URLs.
 * <p/>
 * Handles:
 * <ul>
 *   <li>Path-based multiwiki: {@code http://server/(ignorePrefix)/wiki/wikiname/type/action/space/page/attachment}</li>
 *   <li>Domain-based multiwiki: {@code http://server/(ignorePrefix)/type/action/space/page/attachment}</li>
 * </ul>
 *
 * @version $Id$
 * @since 5.2M1
 */
@Component
@Named("standard")
@Singleton
public class ExtendedURLEntityResourceFactory implements ResourceFactory<ExtendedURL, EntityResource>
{
    /**
     * Used to extract the wiki reference from the URL.
     */
    @Inject
    private WikiReferenceExtractor wikiExtractor;

    /**
     * Used to resolve blanks in entity references when the URL doesn't specify all parts of an entity reference.
     */
    @Inject
    private EntityReferenceResolver<EntityReference> defaultReferenceEntityReferenceResolver;

    /**
     * Used to get the configured entity path prefix from the URL to allow for short URLs.
     */
    @Inject
    private StandardURLConfiguration configuration;

    @Override
    public EntityResource createResource(ExtendedURL url, Map<String, Object> parameters)
        throws ResourceCreationException, UnsupportedResourceException
    {
        EntityResource entityURL;

        // Extract the wiki part.
        // The location of the wiki name depends on whether the wiki is configured to use domain-based multiwiki or
        // path-based multiwiki. If domain-based multiwiki then extract the wiki reference from the domain, otherwise
        // extract it from the path.
        Pair<WikiReference, Boolean> extractionResult = this.wikiExtractor.extract(url);
        WikiReference wikiReference = extractionResult.getLeft();
        boolean isActuallyPathBased = extractionResult.getRight();

        // Remove all required segments till we are at the level of the action part.
        normalizeSegments(url, isActuallyPathBased);

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

        entityURL = new EntityResource(buildEntityReference(wikiReference, spaceName, pageName, attachmentName));
        entityURL.setAction(action);

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
     * Removes path segments till we reach the level of the action to perform on the Entity.
     *
     * @param url the URL containing the path segments and that will get modified
     * @param isActuallyPathBased if true then the passed URL represents a path-based URL
     */
    private void normalizeSegments(ExtendedURL url, boolean isActuallyPathBased)
    {
        // In Path based it means removing 2 segments:
        // Example of path-based URL: wiki/wikiname/action/space/page
        // Thus removing 2 segments means keeping: action/space/page
        if (isActuallyPathBased) {
            url.getSegments().remove(0);
            url.getSegments().remove(0);
        } else {
            // In Domain based, we still need to remove one segment since the first segment will contain the type
            // (e.g. "bin"). However, since we want to support Short URLs and allow the user to not specify the type
            // prefix, we only remove the segment if its value is of the configured value.
            // Note that we also always support "bin" in order to make it easy for the user so that he doesn't have to
            // change all the URLs everywhere (like the error page URL in web.xml, etc).
            String entityPathPrefix = this.configuration.getEntityPathPrefix();
            String firstSegment = url.getSegments().get(0);
            if (firstSegment.equals(entityPathPrefix) || firstSegment.equals("bin")) {
                url.getSegments().remove(0);
            }
        }
    }

    /**
     * Copies query string parameters from the passed {@link ExtendedURL} to the passed {@link EntityResource}.
     *
     * @param source the source URL from where to get the query string parameters
     * @param target the {@link EntityResource} on which to copy the query string parameters
     */
    private void copyParameters(ExtendedURL source, EntityResource target)
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
