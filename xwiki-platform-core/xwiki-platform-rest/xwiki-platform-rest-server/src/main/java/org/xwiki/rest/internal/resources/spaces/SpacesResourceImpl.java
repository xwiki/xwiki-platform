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
package org.xwiki.rest.internal.resources.spaces;

import java.util.List;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.QueryFilter;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Spaces;
import org.xwiki.rest.resources.spaces.SpacesResource;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.spaces.SpacesResourceImpl")
public class SpacesResourceImpl extends XWikiResource implements SpacesResource
{
    @Override
    public Spaces getSpaces(String wikiName, Integer start, Integer number)
            throws XWikiRestException
    {
        Spaces spaces = objectFactory.createSpaces();

        try {
            List<String> spaceNames = queryManager.getNamedQuery("getSpaces").addFilter(
                    componentManager.<QueryFilter>getInstance(QueryFilter.class, "hidden")).setOffset(start)
                    .setLimit(number).setWiki(wikiName).execute();

            for (String spaceName : spaceNames) {
                List<String> spaceList = Utils.getSpacesFromSpaceId(spaceName);
                String homeId = Utils.getPageId(wikiName, spaceList, "WebHome");
                Document home = null;

                XWiki xwikiApi = Utils.getXWikiApi(componentManager);
                if (xwikiApi.hasAccessLevel("view", homeId)) {
                    if (xwikiApi.exists(homeId)) {
                        home = Utils.getXWikiApi(componentManager).getDocument(homeId);
                    }
                    spaces.getSpaces().add(DomainObjectFactory.createSpace(objectFactory, uriInfo.getBaseUri(),
                        wikiName, spaceList, home));
                }
            }
        } catch (Exception e) {
            throw new XWikiRestException(e);
        }

        return spaces;
    }
}
