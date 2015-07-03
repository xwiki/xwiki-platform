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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.resource.internal.entity.EntityResourceActionLister;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.AbstractResourceReferenceResolver;
import org.xwiki.url.internal.standard.StandardURLConfiguration;

/**
 * Common code for Entity Resource Reference Resolvers.
 *
 * @version $Id$
 * @since 6.3M1
 */
public abstract class AbstractEntityResourceReferenceResolver extends AbstractResourceReferenceResolver
{
    private static final String VIEW_ACTION = "view";

    private static final String DOWNLOAD_ACTION = "download";

    private static final String DELATTACHMENT_ACTION = "delattachment";

    private static final String VIEWATTACHREV_ACTION = "viewattachrev";

    /**
     * List of Actions which use URLs of the format {@code /(actionname)/space1/space2/page/filename}.
     */
    private static final List<String> FILE_ACTION_LIST =
        Arrays.asList(DOWNLOAD_ACTION, DELATTACHMENT_ACTION, VIEWATTACHREV_ACTION);

    private StandardURLConfiguration configuration;

    private EntityResourceActionLister entityResourceActionLister;

    /**
     * Used to resolve blanks in entity references when the URL doesn't specify all parts of an entity reference.
     */
    private EntityReferenceResolver<EntityReference> defaultReferenceEntityReferenceResolver;

    protected abstract WikiReference extractWikiReference(ExtendedURL url);

    @Override
    public EntityResourceReference resolve(ExtendedURL extendedURL, ResourceType type, Map<String, Object> parameters)
        throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        EntityResourceReference reference = null;

        // Extract the wiki reference from the URL
        WikiReference wikiReference = extractWikiReference(extendedURL);

        // - 0 segment:
        //   - "": (default space).(default page), "view" action
        // - 1 segment:
        //   - "/": (default space).(default page), "view" action
        //   - "/space": space.(default page), "view" action
        //   - "/edit": (default space).(default page), "edit" action
        // - 2 segments:
        //   - "/space/": space.(default page), "view" action
        //   - "/space/page" ("view" hidden, "space" != action name): space.page, "view" action
        //   - "/view/space" ("view" shown): space.(default page), "view" action
        // - 3 segments:
        //   - "/space1/space2/" ("view" hidden, "space1" != action name): space1.space2.(default page), "view" action
        //   - "/space1/space2/page" ("view" hidden, "space1" != action name): space1.space2.page, "view" action.
        //   - "/view/space/page" ("view" hidden "space" != action name): view.space.page, "view" action.
        //   - "/view/space/page" ("view" shown): space.page, "view" action.
        //   - "/edit/space/page" ("view" hidden, 1st segment is an action name != "view"): space.page, "edit" action
        //     (or any action other than "view")
        //   - "/edit/space/page" ("view" shown): space.page, "edit" action (or any action other than "view").
        //     Note: no need to check 1st segment name in this case.
        //   - "/view/edit/page" ("view hidden", 1st segment is "view"): edit.page, "view" action.
        //     Note: URL serialization must generate a 1st segment named "view" since "edit" is a reserved name for a
        //     space (it's an action name)
        // - 4 segments (or more):
        //   - "/space1/space2/space3/" ("view" hidden, "space1" != action name): space1.space2.space3.(default page),
        //     "view" action
        //   - "/space1/space2/space3/page" ("view" hidden, "space1" != action name): space1.space2.space3.page,
        //     "view" action
        //   - "/view/space1/space2/page" ("view" shown): space1.space2.page, "view" action
        //   - "/edit/space1/space2/page ("view" hidden, 1st segment is an action name != "view"):
        //     space1.space2.page, "edit" action (or any action other than "view")
        //   - "/download/space/page/attachment" (1st segment is "download"): space.page@attachment, "download" action
        //     Note: if there are segments after "attachment" then they are ignored
        //   - "/view/download/space/page" ("view" hidden, 1st segment is "view"): download.space.page, "view" action
        //     Note: URL serialization must generate a 1st segment named "view" since "download" is a reserved name for
        //     a space (it's an action name)
        //   - "/delattachment/space/page/attachment (1st segment is "delattachment"): space.page@attachment,
        //     "delattachment" action. Note: if there are segments after "delattachment" then they are ignored
        //   - "/viewattachrev/space/page/attachment (1st segment is "viewattachrev"): space.page@attachment,
        //     "viewattachrev" action. Note: if there are segments after "viewattachrev" then they are ignored

        List<String> pathSegments = extendedURL.getSegments();
        List<String> spaceNames = null;
        String pageName = null;
        String attachmentName = null;
        String action = VIEW_ACTION;

        if (pathSegments.size() != 0) {
            String firstSegment = pathSegments.get(0);
            action = firstSegment;
            // Generic parsing
            // Handle actions specifying an attachment.
            if (FILE_ACTION_LIST.contains(firstSegment) && pathSegments.size() >= 4) {
                // Last segment is the attachment
                attachmentName = pathSegments.get(pathSegments.size() - 1);
                // Last but one segment is the page name
                pageName = pathSegments.get(pathSegments.size() - 2);
                // All segments in between are the space names
                spaceNames = extractSpaceNames(pathSegments, 1, pathSegments.size() - 3);
            } else {
                // Handle actions not specifying any attachment.
                Pair<String, Integer> actionAndStartPosition =
                    computeActionAndStartPosition(firstSegment, pathSegments, action);
                action = actionAndStartPosition.getLeft();
                int startPosition = actionAndStartPosition.getRight();
                // Normally the last segment is always the page name but we want to handle a special case when we
                // have "/view/something" and we wish in this case to consider that "something" is the space. This
                // is to handle Nested Documents, so that the user can have a top level Nested Document
                // (something.WebHome) and access it from /view/something. If we didn't handle this special case
                // the user would get Main.something and thus wouldn't be able to access something.WebHome. He'd
                // need to use /view/something/ which is not natural in the Nested Document mode.
                if (pathSegments.size() - startPosition == 1) {
                    spaceNames = Arrays.asList(pathSegments.get(startPosition));
                } else {
                    // Last segment is the page name
                    pageName = pathSegments.get(pathSegments.size() - 1);
                    // All segments in between are the space names
                    spaceNames = extractSpaceNames(pathSegments, startPosition, pathSegments.size() - 2);
                }
            }
        }

        if (reference == null) {
            reference = new EntityResourceReference(
                buildEntityReference(wikiReference, spaceNames, pageName, attachmentName),
                EntityResourceAction.fromString(action));
        }

        copyParameters(extendedURL, reference);

        return reference;
    }

    private Pair<String, Integer> computeActionAndStartPosition(String firstSegment, List<String> pathSegments,
        String currentAction)
    {
        String action = currentAction;
        int startPosition = 1;

        if (this.configuration.isViewActionHidden()) {
            if (VIEW_ACTION.equals(firstSegment)) {
                // Does the next segment have an action name?
                String secondSegment = pathSegments.get(1);
                if (this.entityResourceActionLister.listActions().contains(secondSegment)) {
                    action = VIEW_ACTION;
                } else {
                    startPosition = 0;
                }
            } else {
                if (!this.entityResourceActionLister.listActions().contains(firstSegment)) {
                    action = VIEW_ACTION;
                    startPosition = 0;
                }
            }
        }

        return new ImmutablePair<>(action, startPosition);
    }

    private List<String> extractSpaceNames(List<String> pathSegments, int startPosition, int stopPosition)
    {
        if (stopPosition < 0) {
            return null;
        }

        List<String> spaceNames = new ArrayList<>();
        ListIterator<String> iterator = pathSegments.listIterator(startPosition);
        int total = stopPosition - startPosition + 1;
        int count = 0;
        while (count < total) {
            spaceNames.add(iterator.next());
            count++;
        }
        return spaceNames;
    }

    /**
     * Normalize the extracted space/page to resolve empty/null values and replace them with default values.
     *
     * @param wikiReference the wiki reference as extracted from the URL
     * @param spaceNames the space names as extracted from the URL (can be empty or null)
     * @param pageName the page name as extracted from the URL (can be empty or null)
     * @param attachmentName the attachment name as extracted from the URL (can be empty or null)
     * @return the absolute Entity Reference
     */
    private EntityReference buildEntityReference(WikiReference wikiReference, List<String> spaceNames, String pageName,
        String attachmentName)
    {
        EntityReference reference = wikiReference;
        EntityType entityType = EntityType.DOCUMENT;
        if (spaceNames != null && !spaceNames.isEmpty()) {
            EntityReference parent = reference;
            for (String spaceName : spaceNames) {
                if (!StringUtils.isEmpty(spaceName)) {
                    reference = new EntityReference(spaceName, EntityType.SPACE, parent);
                    parent = reference;
                }
            }
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
}
