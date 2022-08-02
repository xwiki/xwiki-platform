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

import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.internal.Utils;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
public class BaseObjectsResource extends XWikiResource
{
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver =
        com.xpn.xwiki.web.Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");

    protected BaseObject getBaseObject(Document doc, String className, int objectNumber) throws XWikiException
    {
        XWikiDocument xwikiDocument =
            Utils.getXWiki(componentManager).getDocument(doc.getDocumentReference(),
                Utils.getXWikiContext(componentManager));

        return xwikiDocument.getObject(className, objectNumber);
    }

    protected List<BaseObject> getBaseObjects(Document doc) throws XWikiException
    {
        List<BaseObject> objectList = new ArrayList<BaseObject>();

        XWikiDocument xwikiDocument =
            Utils.getXWiki(componentManager).getDocument(doc.getDocumentReference(),
                Utils.getXWikiContext(componentManager));

        for (List<BaseObject> xwikiObjects : xwikiDocument.getXObjects().values()) {
            objectList.addAll(xwikiObjects);
        }

        return objectList;
    }

    protected List<BaseObject> getBaseObjects(Document doc, String className) throws XWikiException
    {
        XWikiDocument xwikiDocument = Utils.getXWiki(componentManager).getDocument(doc.getDocumentReference(),
            Utils.getXWikiContext(componentManager));

        List<BaseObject> xwikiObjects = xwikiDocument.getXObjects(xwikiDocument.resolveClassReference(className));

        // XWikiDocument#getXObjects return internal list so we make sure to return a safe one
        return xwikiObjects != null ? new ArrayList<BaseObject>(xwikiObjects) : Collections.<BaseObject>emptyList();
    }
}
