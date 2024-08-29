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
package org.xwiki.rest.internal.resources.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
public class BaseObjectsResource extends XWikiResource
{
    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    protected BaseObject getBaseObject(Document doc, String className, int objectNumber) throws XWikiException
    {
        XWikiContext xWikiContext = this.xcontextProvider.get();
        XWikiDocument xwikiDocument = xWikiContext.getWiki().getDocument(doc.getDocumentReference(), xWikiContext);

        return xwikiDocument.getObject(className, objectNumber);
    }

    protected List<BaseObject> getBaseObjects(DocumentReference documentReference, String version)
        throws XWikiException
    {
        XWikiDocument xwikiDocument = this.documentRevisionProvider.getRevision(documentReference, version);

        return getBaseObjectList(xwikiDocument);
    }

    protected List<BaseObject> getBaseObjects(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext xWikiContext = this.xcontextProvider.get();
        XWikiDocument xwikiDocument = xWikiContext.getWiki().getDocument(documentReference, xWikiContext);

        return getBaseObjectList(xwikiDocument);
    }

    private static List<BaseObject> getBaseObjectList(XWikiDocument xwikiDocument)
    {
        return xwikiDocument.getXObjects().values().stream().flatMap(List::stream).toList();
    }

    protected List<BaseObject> getBaseObjects(Document doc, String className) throws XWikiException
    {
        XWikiContext xWikiContext = this.xcontextProvider.get();
        XWikiDocument xwikiDocument = xWikiContext.getWiki().getDocument(doc.getDocumentReference(), xWikiContext);

        List<BaseObject> xwikiObjects = xwikiDocument.getXObjects(xwikiDocument.resolveClassReference(className));

        // XWikiDocument#getXObjects return internal list so we make sure to return a safe one
        return xwikiObjects != null ? new ArrayList<>(xwikiObjects) : Collections.emptyList();
    }
}
