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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Space;
import org.xwiki.rest.resources.spaces.SpaceResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.spaces.SpaceResourceImpl")
public class SpaceResourceImpl extends XWikiResource implements SpaceResource
{
    @Override
    public Space getSpace(String wikiName, String spaceName, String lastSpaceName) throws XWikiRestException
    {
        String database = Utils.getXWikiContext(componentManager).getWikiId();
        // Because the path "/wikis/{wikiName}/{spaceName: .*}" was not working, we use
        // "/wikis/{wikiName}/{spaceName: .*}spaces/{lastSpaceName}", which means we have the last space in a parameter,
        // and the parent spaces in an other parameter. 
        // The parent spaces might exist (e.g. Main.SubSpace) or not (e.g. Main), so we handle the 2 cases.
        List<String> spaces = 
                StringUtils.isNotEmpty(spaceName) ? parseSpaceSegments(spaceName) : new ArrayList<String>();
        spaces.add(lastSpaceName);

        try {
            Utils.getXWikiContext(componentManager).setWikiId(wikiName);

            String homeId = Utils.getPageId(wikiName, spaces, "WebHome");
            Document home = null;

            if (Utils.getXWikiApi(componentManager).exists(homeId)) {
                home = Utils.getXWikiApi(componentManager).getDocument(homeId);
            }

            return DomainObjectFactory.createSpace(objectFactory, uriInfo.getBaseUri(), wikiName, spaces, home);
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(database);
        }
    }
}
