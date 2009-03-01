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
package org.xwiki.rest.resources.pages;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.xwiki.rest.Relations;
import org.xwiki.rest.Utils;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Tag;
import org.xwiki.rest.model.jaxb.Tags;
import org.xwiki.rest.resources.tags.PagesForTagsResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.tag.TagPlugin;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/tags")
public class PageTagsResource extends ModifiablePageResource
{
    public PageTagsResource(@Context UriInfo uriInfo)
    {
        super(uriInfo);
    }

    @GET
    public Tags getPageTags(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName) throws XWikiException
    {
        TagPlugin tagPlugin = (TagPlugin) xwiki.getPlugin("tag", xwikiContext);

        String pageId = Utils.getPageId(wikiName, spaceName, pageName);
        List<String> tagNames = tagPlugin.getTagsFromDocument(pageId, xwikiContext);

        Tags tags = objectFactory.createTags();
        for (String tagName : tagNames) {
            Tag tag = objectFactory.createTag();
            tag.setName(tagName);

            String tagUri =
                UriBuilder.fromUri(uriInfo.getBaseUri()).path(PagesForTagsResource.class).build(wikiName, tagName)
                    .toString();
            Link tagLink = objectFactory.createLink();
            tagLink.setHref(tagUri);
            tagLink.setRel(Relations.TAG);
            tag.getLinks().add(tagLink);

            tags.getTags().add(tag);
        }

        return tags;
    }

    @PUT
    public Response setTags(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, Tags tags) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

        Document doc = documentInfo.getDocument();

        List<String> tagNames = new ArrayList<String>();
        for (Tag tag : tags.getTags()) {
            tagNames.add(tag.getName());
        }

        XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
        BaseObject xwikiObject = xwikiDocument.getObject("XWiki.TagClass", 0);

        if (xwikiObject == null) {
            int objectNumber = xwikiDocument.createNewObject("XWiki.TagClass", xwikiContext);
            xwikiObject = xwikiDocument.getObject("XWiki.TagClass", objectNumber);
            if (xwikiObject == null) {
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }

            // We must initialize all the fields to an empty value in order to correctly create the object
            BaseClass xwikiClass = xwiki.getClass(xwikiObject.getClassName(), xwikiContext);
            for (java.lang.Object propertyNameObject : xwikiClass.getPropertyNames()) {
                String propertyName = (String) propertyNameObject;
                xwikiObject.set(propertyName, "", xwikiContext);
            }
        }

        xwikiObject.set("tags", tagNames, xwikiContext);

        doc.save();

        return Response.status(Status.ACCEPTED).entity(tags).build();
    }
}
