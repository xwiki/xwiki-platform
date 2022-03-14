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
package com.xpn.xwiki.internal.web;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * TODO: document me.
 *
 * @version $Id$
 * @since 13.10.4
 * @since 14.2RC1
 */
@Component
@Singleton
@Named("view")
public class ViewDocExistValidator implements DocExistValidator
{
    private static final String VIEWER_REQUEST_PARAMETER = "viewer";

    private static final String REV_REQUEST_PARAMETER = "rev";

    private static final String VIEWER_RECYCLEBIN = "recyclebin";

    private static final String VIEWER_CHILDREN = "children";

    private static final String VIEWER_SIBLINGS = "siblings";

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    // @return {@code true} if we should return a 404 
    @Override
    public boolean docExist(XWikiDocument doc, XWikiContext context)
    {
        boolean result = false;
        XWikiRequest request = context.getRequest();
        String rev = request.get(REV_REQUEST_PARAMETER);
        boolean hasRev = rev != null; // && rev.startsWith("deleted:");
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
        try {
            XWikiDocument revision = this.documentRevisionProvider.getRevision(doc, rev);
            return revision != null;
        } catch (XWikiException e) {
            // TODO...
            e.printStackTrace();
        }
        return false;
    }
}
