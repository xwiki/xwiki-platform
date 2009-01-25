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
package org.xwiki.rest.resources.comments;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.xwiki.rest.Constants;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.RangeIterable;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.Comment;
import org.xwiki.rest.model.Comments;
import org.xwiki.rest.model.XStreamFactory;

import com.thoughtworks.xstream.XStream;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
public class CommentsResource extends XWikiResource
{

    @Override
    public Representation represent(Variant variant)
    {
        try {
            DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), true);
            if (documentInfo == null) {
                /* If the document doesn't exist send a not found header */
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return null;
            }

            Document doc = documentInfo.getDocument();

            /* Check if we have access to it */
            if (doc == null) {
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                return null;
            }

            Comments comments = new Comments();

            Vector<com.xpn.xwiki.api.Object> xwikiComments = doc.getComments();

            Form queryForm = getRequest().getResourceRef().getQueryAsForm();
            RangeIterable<com.xpn.xwiki.api.Object> ri =
                new RangeIterable<com.xpn.xwiki.api.Object>(xwikiComments, Utils.parseInt(queryForm
                    .getFirstValue(Constants.START_PARAMETER), 0), Utils.parseInt(queryForm
                    .getFirstValue(Constants.NUMBER_PARAMETER), -1));

            for (com.xpn.xwiki.api.Object xwikiComment : ri) {
                comments.addComment(DomainObjectFactory.createComment(getRequest(), resourceClassRegistry, doc,
                    xwikiComment));
            }

            return getRepresenterFor(variant).represent(getContext(), getRequest(), getResponse(), comments);
        } catch (Exception e) {
            e.printStackTrace();
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        }

        return null;
    }

    @Override
    public boolean allowPost()
    {
        return true;
    }

    @Override
    public void handlePost()
    {
        MediaType mediaType = getRequest().getEntity().getMediaType();

        DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), false);
        if (documentInfo == null) {
            /* Should not happen since we requested not to fail if the document doesn't exist */
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            return;

        }

        Document doc = documentInfo.getDocument();
        /* If the doc is null we don't have the rights to access it. */
        if (doc == null) {
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
            return;
        }

        /* If the doc is locked then return */
        if (doc.getLocked()) {
            getResponse().setStatus(Status.CLIENT_ERROR_LOCKED);
            return;
        }

        try {
            int id = doc.createNewObject("XWiki.XWikiComments");
            com.xpn.xwiki.api.Object commentObject = doc.getObject("XWiki.XWikiComments", id);
            commentObject.set("author", xwikiUser);
            commentObject.set("date", new Date());

            /* Process the entity */
            if (MediaType.TEXT_PLAIN.equals(mediaType)) {
                try {
                    commentObject.set("comment", getRequest().getEntity().getText());

                    doc.save();
                } catch (IOException e) {
                    getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                    return;
                }
            } else if (MediaType.APPLICATION_XML.equals(mediaType)) {
                XStream xstream = XStreamFactory.getXStream();

                Comment comment = null;

                /* If we receive an XML that is not convertible to a Page object we reject it */
                try {
                    comment = (Comment) xstream.fromXML(getRequest().getEntity().getText());
                } catch (Exception e) {
                    getResponse().setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE);
                    return;
                }

                /* We will only save if something changes... */
                boolean save = false;

                if (comment.getHighlight() != null) {
                    commentObject.set("highlight", comment.getHighlight());
                    save = true;
                }

                if (comment.getText() != null) {
                    commentObject.set("comment", comment.getText());
                    save = true;
                }

                if (save) {
                    doc.save();

                    getResponse().setStatus(Status.SUCCESS_CREATED);

                    /* Set the entity as being the new/updated document XML representation */
                    getResponse().setEntity(
                        new StringRepresentation(Utils.toXml(DomainObjectFactory.createComment(getRequest(),
                            resourceClassRegistry, doc, commentObject)), MediaType.APPLICATION_XML));
                }
            }
        } catch (XWikiException e) {
            if (e.getCode() == XWikiException.ERROR_XWIKI_ACCESS_DENIED) {
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
            } else {
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            }

            return;
        }
    }

}
