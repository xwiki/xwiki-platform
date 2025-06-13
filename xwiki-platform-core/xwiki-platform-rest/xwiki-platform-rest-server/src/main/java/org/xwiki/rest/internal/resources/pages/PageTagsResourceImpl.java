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
package org.xwiki.rest.internal.resources.pages;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Tag;
import org.xwiki.rest.model.jaxb.Tags;
import org.xwiki.rest.resources.pages.PageTagsResource;
import org.xwiki.rest.resources.tags.PagesForTagsResource;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.pages.PageTagsResourceImpl")
public class PageTagsResourceImpl extends ModifiablePageResource implements PageTagsResource
{
    private static final String PROPERTY_TAGS = "tags";

    private static final String TAG_CLASS = "XWiki.TagClass";

    @Override
    public Tags getPageTags(String wikiName, String spaceName, String pageName) throws XWikiRestException
    {
        try {
            String pageId = Utils.getPageId(wikiName, parseSpaceSegments(spaceName), pageName);
            List<String> tagNames = getTagsFromDocument(pageId);

            Tags tags = objectFactory.createTags();
            for (String tagName : tagNames) {
                Tag tag = objectFactory.createTag();
                tag.setName(tagName);

                String tagUri =
                    Utils.createURI(uriInfo.getBaseUri(), PagesForTagsResource.class, wikiName, tagName).toString();
                Link tagLink = objectFactory.createLink();
                tagLink.setHref(tagUri);
                tagLink.setRel(Relations.TAG);
                tag.getLinks().add(tagLink);

                tags.getTags().add(tag);
            }

            return tags;
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    @Override
    public Response setTags(String wikiName, String spaceName, String pageName, Boolean minorRevision, Tags tags)
        throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

            Document doc = documentInfo.getDocument();

            if (!doc.hasAccessLevel("edit", Utils.getXWikiUser(componentManager))) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            List<String> tagNames = new ArrayList<String>();
            for (Tag tag : tags.getTags()) {
                tagNames.add(tag.getName());
            }

            XWikiContext xcontext = getXWikiContext();
            XWiki xwiki = xcontext.getWiki();

            XWikiDocument xwikiDocument = xwiki.getDocument(doc.getDocumentReference(), xcontext);

            // Avoid modifying the cached document
            xwikiDocument = xwikiDocument.clone();

            BaseObject xwikiObject = xwikiDocument.getObject(TAG_CLASS, 0);

            if (xwikiObject == null) {
                int objectNumber = xwikiDocument.createNewObject(TAG_CLASS, xcontext);
                xwikiObject = xwikiDocument.getObject(TAG_CLASS, objectNumber);
                if (xwikiObject == null) {
                    throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
                }

                // We must initialize all the fields to an empty value in order to correctly create the object
                BaseClass xwikiClass = xwiki.getClass(xwikiObject.getClassName(), xcontext);
                for (Object propertyNameObject : xwikiClass.getPropertyNames()) {
                    String propertyName = (String) propertyNameObject;
                    xwikiObject.set(propertyName, "", xcontext);
                }
            }

            xwikiObject.set(PROPERTY_TAGS, tagNames, xcontext);

            xwiki.saveDocument(xwikiDocument, "", Boolean.TRUE.equals(minorRevision), getXWikiContext());

            return Response.status(Status.ACCEPTED).entity(tags).build();
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    private List<String> getTagsFromDocument(String documentId) throws XWikiException
    {
        XWikiDocument document =
            Utils.getXWiki(componentManager).getDocument(documentId, Utils.getXWikiContext(componentManager));
        BaseObject object = document.getObject(TAG_CLASS);
        if (object != null) {
            BaseProperty prop = (BaseProperty) object.safeget(PROPERTY_TAGS);
            if (prop != null) {
                List<String> tags = (List<String>) prop.getValue();
                if (tags != null) {
                    return tags;
                }
            }
        }

        return new ArrayList<String>();
    }
}
