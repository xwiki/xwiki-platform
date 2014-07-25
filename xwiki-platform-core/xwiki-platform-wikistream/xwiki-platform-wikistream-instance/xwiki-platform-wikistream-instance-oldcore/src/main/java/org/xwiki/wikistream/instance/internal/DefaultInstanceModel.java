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
package org.xwiki.wikistream.instance.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wikistream.WikiStreamException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Singleton
public class DefaultInstanceModel implements InstanceModel
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private QueryManager queryManager;

    @Override
    public List<String> getWikis() throws WikiStreamException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        try {
            List<String> wikis = new ArrayList<String>(xcontext.getWiki().getVirtualWikisDatabaseNames(xcontext));
            Collections.sort(wikis);
            return wikis;
        } catch (XWikiException e) {
            throw new WikiStreamException("Failed to get the list of wikis", e);
        }
    }

    @Override
    public List<String> getSpaces(String wiki) throws WikiStreamException
    {
        try {
            return this.queryManager.getNamedQuery("getSpaces").setWiki(wiki).execute();
        } catch (QueryException e) {
            throw new WikiStreamException(String.format("Failed to get the list of spaces in wiki [%s]", wiki), e);
        }
    }

    @Override
    public List<String> getDocuments(String wiki, String space) throws WikiStreamException
    {
        try {
            Query query =
                this.queryManager
                    .createQuery(
                        "select distinct doc.name from Document doc where doc.space = :space order by doc.name asc",
                        Query.XWQL);
            query.bindValue("space", space);
            query.setWiki(wiki);

            return query.execute();
        } catch (QueryException e) {
            throw new WikiStreamException(String.format(
                "Failed to get the list of documents in wiki [%s] and space [%s]", wiki, space), e);
        }
    }
}
