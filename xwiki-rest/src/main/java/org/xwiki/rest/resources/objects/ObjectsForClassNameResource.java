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

import org.restlet.data.Form;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xwiki.rest.Constants;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.RangeIterable;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.Objects;

import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
public class ObjectsForClassNameResource extends XWikiResource
{
    @Override
    public Representation represent(Variant variant)
    {
        String className = (String) getRequest().getAttributes().get(Constants.CLASS_NAME_PARAMETER);

        DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), true, false);
        if (documentInfo == null) {
            return null;
        }

        Document doc = documentInfo.getDocument();

        Map<String, Vector<com.xpn.xwiki.api.Object>> classToObjectsMap = doc.getxWikiObjects();

        Objects objects = new Objects();

        List<com.xpn.xwiki.api.Object> objectList = new ArrayList<com.xpn.xwiki.api.Object>();
        for (String xwikiClassName : classToObjectsMap.keySet()) {
            if (xwikiClassName.equals(className)) {
                Vector<com.xpn.xwiki.api.Object> xwikiObjects = classToObjectsMap.get(xwikiClassName);
                for (com.xpn.xwiki.api.Object object : xwikiObjects) {
                    objectList.add(object);
                }
            }
        }

        Form queryForm = getRequest().getResourceRef().getQueryAsForm();
        RangeIterable<com.xpn.xwiki.api.Object> ri =
            new RangeIterable<com.xpn.xwiki.api.Object>(objectList, Utils.parseInt(queryForm
                .getFirstValue(Constants.START_PARAMETER), 0), Utils.parseInt(queryForm
                .getFirstValue(Constants.NUMBER_PARAMETER), -1));

        for (com.xpn.xwiki.api.Object object : ri) {
            objects.addObjectSummary(DomainObjectFactory.createObjectSummary(getRequest(), resourceClassRegistry, doc,
                object));
        }

        return getRepresenterFor(variant).represent(getContext(), getRequest(), getResponse(), objects);
    }

}
