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
package org.xwiki.rest.resources.tags;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.UriBuilder;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.rest.Relations;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Tag;
import org.xwiki.rest.model.jaxb.Tags;

@Component("org.xwiki.rest.resources.tags.TagsResource")
@Path("/wikis/{wikiName}/tags")
public class TagsResource extends XWikiResource
{
    @GET
    public Tags getTags(@PathParam("wikiName") String wikiName) throws QueryException
    {
        String database = Utils.getXWikiContext(componentManager).getDatabase();

        Tags tags = objectFactory.createTags();

        /* This try is just needed for executing the finally clause. */
        try {
            Utils.getXWikiContext(componentManager).setDatabase(wikiName);

            List<String> tagNames = getAllTags();

            for (String tagName : tagNames) {
                Tag tag = objectFactory.createTag();
                tag.setName(tagName);

                String tagUri =
                    UriBuilder.fromUri(uriInfo.getBaseUri()).path(PagesForTagsResource.class).build(wikiName, tagName)
                        .toString();
                Link tagLink = objectFactory.createLink();
                tagLink.setHref(tagUri);
                tagLink.setRel(Relations.TAG);
                tag.getLinks().add(tagLink);

                tags.getTags().add(tag);
            }
        } finally {
            Utils.getXWikiContext(componentManager).setDatabase(database);
        }

        return tags;
    }

    private List<String> getAllTags() throws QueryException
    {
        String query =
            "select distinct elements(prop.list) from BaseObject as obj, "
                + "DBStringListProperty as prop where obj.className='XWiki.TagClass' "
                + "and obj.id=prop.id.id and prop.id.name='tags'";

        List<String> tags = queryManager.createQuery(query, Query.HQL).execute();
        Collections.sort(tags, String.CASE_INSENSITIVE_ORDER);

        return tags;
    }
}
