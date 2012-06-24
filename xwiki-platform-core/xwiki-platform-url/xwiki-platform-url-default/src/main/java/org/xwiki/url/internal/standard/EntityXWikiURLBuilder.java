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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.url.XWikiEntityURL;
import org.xwiki.url.XWikiURL;
import org.xwiki.url.standard.XWikiURLBuilder;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Named("entity")
@Singleton
public class EntityXWikiURLBuilder implements XWikiURLBuilder
{
    @Inject
    private EntityReferenceResolver<EntityReference> defaultReferenceEntityReferenceResolver;
    
    @Override
    public XWikiURL build(WikiReference wikiReference, List<String> pathSegments)
    {
        XWikiEntityURL entityURL;

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

        // Normalize the extracted space/page to resolve empty/null values and replace them with default values.
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
        reference = this.defaultReferenceEntityReferenceResolver.resolve(reference, entityType);

        entityURL = new XWikiEntityURL(reference);
        entityURL.setAction(action);

        return entityURL;
    }
}
