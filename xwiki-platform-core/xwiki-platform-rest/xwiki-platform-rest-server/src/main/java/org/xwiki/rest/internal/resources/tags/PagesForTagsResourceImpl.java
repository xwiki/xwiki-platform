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
package org.xwiki.rest.internal.resources.tags;

import java.util.List;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.rest.resources.tags.PagesForTagsResource;

import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.tags.PagesForTagsResourceImpl")
public class PagesForTagsResourceImpl extends XWikiResource implements PagesForTagsResource
{
    @Override
    public Pages getTags(String wikiName, String tagNames, Integer start, Integer number, Boolean withPrettyNames)
            throws XWikiRestException
    {
        String database = Utils.getXWikiContext(componentManager).getWikiId();

        int limit = validateAndGetLimit(number);

        try {
            Pages pages = objectFactory.createPages();

            Utils.getXWikiContext(componentManager).setWikiId(wikiName);

            String[] tagNamesArray = tagNames.split(",");
            List<String> documentNames = getDocumentsWithTags(tagNamesArray, start, limit);

            for (String documentName : documentNames) {
                Document doc = Utils.getXWikiApi(componentManager).getDocument(documentName);
                if (doc != null) {
                    pages.getPageSummaries().add(DomainObjectFactory
                            .createPageSummary(objectFactory, uriInfo.getBaseUri(), doc, Utils.getXWikiApi(
                                    componentManager), withPrettyNames));
                }
            }

            return pages;
        } catch (Exception e) {
            throw new XWikiRestException(e);
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(database);
        }
    }

    private List<String> getDocumentsWithTags(String[] tags, Integer start, Integer limit) throws QueryException
    {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(
            "select distinct doc.fullName from XWikiDocument as doc, BaseObject as obj, DBStringListProperty as prop ");
        queryBuilder.append("where obj.name=doc.fullName and obj.className='XWiki.TagClass' and obj.id=prop.id.id ");
        queryBuilder.append("and prop.id.name='tags' and (");

        for (int i = 0; i < tags.length; i++) {
            if (i > 0) {
                queryBuilder.append(" or ");
            }
            queryBuilder.append(":tag").append(i).append(" in elements(prop.list)");
        }

        queryBuilder.append(") order by doc.fullName asc");

        Query query = this.queryManager.createQuery(queryBuilder.toString(), Query.HQL)
            .setLimit(limit)
            .setOffset(start);

        for (int i = 0; i < tags.length; i++) {
            query.bindValue("tag" + i, tags[i].trim());
        }

        return query.execute();
    }
}
