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
package org.xwiki.rest.internal.resources.wikis;

import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Wikis;
import org.xwiki.rest.resources.wikis.WikisResource;
import org.xwiki.rest.resources.wikis.WikisSearchQueryResource;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.internal.resources.wikis.WikisResourceImpl")
public class WikisResourceImpl extends XWikiResource implements WikisResource
{
    @Override
    public Wikis getWikis() throws XWikiRestException
    {
        try {
            String mainWiki = Utils.getXWikiContext(componentManager).getMainXWiki();

            List<String> databaseNames = Utils.getXWiki(componentManager).getVirtualWikisDatabaseNames(
                    Utils.getXWikiContext(componentManager));

            /* The main wiki, usually "xwiki", doesn't have a wiki descriptor. So if it's not in the list returned by
             getVirtualWikisDatabaseNames add it. */
            if (!databaseNames.contains(mainWiki)) {
                databaseNames.add(mainWiki);
            }

            Wikis wikis = objectFactory.createWikis();

            for (String databaseName : databaseNames) {
                wikis.getWikis().add(DomainObjectFactory.createWiki(objectFactory, uriInfo.getBaseUri(), databaseName));
            }

            String queryUri = Utils.createURI(uriInfo.getBaseUri(), WikisSearchQueryResource.class).toString();
            Link queryLink = objectFactory.createLink();
            queryLink.setHref(queryUri);
            queryLink.setRel(Relations.QUERY);
            wikis.getLinks().add(queryLink);

            return wikis;
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
}
