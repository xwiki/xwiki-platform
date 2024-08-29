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
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.RangeIterable;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.resources.objects.ObjectsAtPageVersionResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.objects.ObjectsAtPageVersionResourceImpl")
public class ObjectsAtPageVersionResourceImpl extends BaseObjectsResource implements ObjectsAtPageVersionResource
{
    @Override
    public Objects getObjects(String wikiName, String spaceName, String pageName, String version, Integer start,
            Integer number, Boolean withPrettyNames) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, version, true, false);

            Document doc = documentInfo.getDocument();

            Objects objects = objectFactory.createObjects();

            List<com.xpn.xwiki.objects.BaseObject> objectList =
                getBaseObjects(doc.getDocumentReferenceWithLocale(), version);

            RangeIterable<com.xpn.xwiki.objects.BaseObject> ri =
                    new RangeIterable<com.xpn.xwiki.objects.BaseObject>(objectList, start, number);

            for (com.xpn.xwiki.objects.BaseObject object : ri) {
                /* By deleting objects, some of them might become null, so we must check for this */
                if (object != null) {
                    objects.getObjectSummaries().add(DomainObjectFactory
                            .createObjectSummary(objectFactory, uriInfo.getBaseUri(), Utils.getXWikiContext(
                                    componentManager), doc, object, true, Utils.getXWikiApi(componentManager),
                                    withPrettyNames));
                }
            }

            return objects;
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
}
