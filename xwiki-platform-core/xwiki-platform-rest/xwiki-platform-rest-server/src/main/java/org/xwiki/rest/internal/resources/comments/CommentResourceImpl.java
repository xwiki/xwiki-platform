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
package org.xwiki.rest.internal.resources.comments;

import java.util.Vector;

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Comment;
import org.xwiki.rest.resources.comments.CommentResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.comments.CommentResourceImpl")
public class CommentResourceImpl extends XWikiResource implements CommentResource
{
    @Override
    public Comment getComment(String wikiName, String spaceName, String pageName, Integer id, Integer start,
            Integer number, Boolean withPrettyNames) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

            Document doc = documentInfo.getDocument();

            Vector<com.xpn.xwiki.api.Object> xwikiComments = doc.getComments();

            for (com.xpn.xwiki.api.Object xwikiComment : xwikiComments) {
                if (id.equals(xwikiComment.getNumber())) {
                    return DomainObjectFactory.createComment(objectFactory, uriInfo.getBaseUri(), doc, xwikiComment,
                            Utils.getXWikiApi(componentManager), withPrettyNames);
                }
            }

            throw new WebApplicationException(Status.NOT_FOUND);
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
}
