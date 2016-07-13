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

import java.util.List;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.ObjectSummary;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.resources.objects.AllObjectsForClassNameResource;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.objects.AllObjectsForClassNameResourceImpl")
public class AllObjectsForClassNameResourceImpl extends XWikiResource implements AllObjectsForClassNameResource
{
    @Override
    public Objects getObjects(String wikiName, String className, Integer start, Integer number, String order,
            Boolean withPrettyNames) throws XWikiRestException
    {
        String database = Utils.getXWikiContext(componentManager).getWikiId();

        try {
            Objects objects = new Objects();

            Utils.getXWikiContext(componentManager).setWikiId(wikiName);

            String query =
                    "select doc, obj from BaseObject as obj, XWikiDocument as doc where obj.name=doc.fullName and obj.className=:className";
            if ("date".equals(order)) {
                query += " order by doc.date desc";
            }

            List<Object> queryResult = null;
            queryResult =
                    queryManager.createQuery(query, Query.XWQL).bindValue("className", className).setLimit(number)
                            .setOffset(start).execute();

            for (Object object : queryResult) {
                Object[] fields = (Object[]) object;

                XWikiDocument xwikiDocument = (XWikiDocument) fields[0];
                xwikiDocument.setDatabase(wikiName);
                Document doc = new Document(xwikiDocument, Utils.getXWikiContext(componentManager));
                BaseObject xwikiObject = (BaseObject) fields[1];

                ObjectSummary objectSummary = DomainObjectFactory
                        .createObjectSummary(objectFactory, uriInfo.getBaseUri(), Utils.getXWikiContext(
                                componentManager), doc, xwikiObject, false, Utils.getXWikiApi(componentManager),
                                withPrettyNames);

                objects.getObjectSummaries().add(objectSummary);
            }

            return objects;
        } catch (Exception e) {
            throw new XWikiRestException(e);
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(database);
        }
    }
}
