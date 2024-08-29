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
package org.xwiki.internal.web;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.ViewAction;
import com.xpn.xwiki.web.XWikiRequest;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Implementation of {@link DocExistValidator} for the {@code view} actions.
 *
 * @version $Id$
 * @since 13.10.4
 * @since 14.2RC1
 */
@Component
@Singleton
@Named(ViewAction.VIEW_ACTION)
public class ViewDocExistValidator implements DocExistValidator
{
    private static final String VIEWER_REQUEST_PARAMETER = "viewer";

    private static final String REV_REQUEST_PARAMETER = "rev";

    private static final String VIEWER_RECYCLEBIN = "recyclebin";

    private static final String VIEWER_CHILDREN = "children";

    private static final String VIEWER_SIBLINGS = "siblings";

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    @Inject
    private Logger logger;

    @Override
    public boolean docExist(XWikiDocument doc, XWikiContext context)
    {
        boolean result = false;
        XWikiRequest request = context.getRequest();
        String rev = request.get(REV_REQUEST_PARAMETER);
        boolean hasRev = rev != null;
        if (doc.isNew() && !hasRev) {
            String viewer = request.get(VIEWER_REQUEST_PARAMETER);
            result = !VIEWER_RECYCLEBIN.equals(viewer)
                && !VIEWER_CHILDREN.equals(viewer)
                && !VIEWER_SIBLINGS.equals(viewer);
        } else if (hasRev) {
            return !revisionExists(doc, rev);
        }
        return result;
    }

    private boolean revisionExists(XWikiDocument doc, String rev)
    {
        boolean result;
        try {
            XWikiDocument revisionDoc = this.documentRevisionProvider.getRevision(doc, rev);
            result = revisionDoc != null;
        } catch (XWikiException e) {
            this.logger.warn("Error while accessing document [{}] in revision [{}]. Cause: [{}].",
                doc.getDocumentReference(), rev, getRootCauseMessage(e));
            // There is an error, we consider that the revision doesn't exist.
            result = false;
        }
        return result;
    }
}
