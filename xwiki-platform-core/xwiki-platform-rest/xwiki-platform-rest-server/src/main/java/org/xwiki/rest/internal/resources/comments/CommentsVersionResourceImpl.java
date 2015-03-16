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

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.RangeIterable;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Comments;
import org.xwiki.rest.resources.comments.CommentsVersionResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.comments.CommentsVersionResourceImpl")
public class CommentsVersionResourceImpl extends XWikiResource implements CommentsVersionResource
{
    @Override
    public Comments getCommentsVersion(String wikiName, String spaceName, String pageName, String version,
            Integer start, Integer number, Boolean withPrettyNames) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, version, true, false);

            Document doc = documentInfo.getDocument();

            Comments comments = objectFactory.createComments();

            Vector<com.xpn.xwiki.api.Object> xwikiComments = doc.getComments();

            RangeIterable<com.xpn.xwiki.api.Object> ri =
                    new RangeIterable<com.xpn.xwiki.api.Object>(xwikiComments, start, number);

            for (com.xpn.xwiki.api.Object xwikiComment : ri) {
                comments.getComments().add(DomainObjectFactory
                        .createComment(objectFactory, uriInfo.getBaseUri(), doc, xwikiComment,
                                Utils.getXWikiApi(componentManager), withPrettyNames));
            }

            return comments;
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
}
