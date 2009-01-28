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

import java.util.Vector;

import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xwiki.rest.Constants;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.RangeIterable;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.Comments;

import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
public class CommentsVersionResource extends XWikiResource
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
                    xwikiComment, true));
            }

            return getRepresenterFor(variant).represent(getContext(), getRequest(), getResponse(), comments);
        } catch (Exception e) {
            e.printStackTrace();
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        }

        return null;
    }

}
