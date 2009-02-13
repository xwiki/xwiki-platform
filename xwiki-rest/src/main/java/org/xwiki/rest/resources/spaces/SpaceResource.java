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
package org.xwiki.rest.resources.spaces;

import java.util.List;

import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xwiki.rest.Constants;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.Space;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
public class SpaceResource extends XWikiResource
{
    @Override
    public Representation represent(Variant variant)
    {
        String database = xwikiContext.getDatabase();
        
        try {
            String wiki = (String) getRequest().getAttributes().get(Constants.WIKI_NAME_PARAMETER);            
            String spaceName = (String) getRequest().getAttributes().get(Constants.SPACE_NAME_PARAMETER);
            xwikiContext.setDatabase(wiki);            

            List<String> docNames = xwikiApi.getSpaceDocsName(spaceName);
            String home = String.format("%s.WebHome", spaceName);
            String homeXWikiUrl = null;

            if (!xwikiApi.exists(home)) {
                home = null;
            } else {
                Document doc = xwikiApi.getDocument(home);
                if (doc != null) {
                    homeXWikiUrl = doc.getExternalURL("view");
                }
            }

            Space space =
                DomainObjectFactory.createSpace(getRequest(), resourceClassRegistry, wiki, spaceName, home,
                    homeXWikiUrl, docNames.size());

            return getRepresenterFor(variant).represent(getContext(), getRequest(), getResponse(), space);
        } catch (XWikiException e) {
            e.printStackTrace();
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        } finally {
            xwiki.setDatabase(database);
        }

        return null;
    }

}
