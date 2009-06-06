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
package org.xwiki.rest.resources.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.RangeIterable;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Objects;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/objects/{className}")
public class ObjectsForClassNameResource extends XWikiResource
{
    @GET
    public Objects getObjects(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @PathParam("className") String className,
        @QueryParam("start") @DefaultValue("0") Integer start, @QueryParam("number") @DefaultValue("-1") Integer number)
        throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

        Document doc = documentInfo.getDocument();

        Objects objects = objectFactory.createObjects();

        List<com.xpn.xwiki.objects.BaseObject> objectList = new ArrayList<com.xpn.xwiki.objects.BaseObject>();

        XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);

        Map<String, Vector<com.xpn.xwiki.objects.BaseObject>> classToObjectsMap = xwikiDocument.getxWikiObjects();

        Vector<com.xpn.xwiki.objects.BaseObject> xwikiObjects = classToObjectsMap.get(className);
        for (com.xpn.xwiki.objects.BaseObject object : xwikiObjects) {
            objectList.add(object);
        }

        RangeIterable<com.xpn.xwiki.objects.BaseObject> ri =
            new RangeIterable<com.xpn.xwiki.objects.BaseObject>(objectList, start, number);

        for (com.xpn.xwiki.objects.BaseObject object : ri) {
            objects.getObjectSummaries().add(
                DomainObjectFactory.createObjectSummary(objectFactory, uriInfo.getBaseUri(), xwikiContext, doc, object,
                    false));
        }

        return objects;
    }

}
