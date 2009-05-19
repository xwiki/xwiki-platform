package org.xwiki.rest.resources.tags;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.UriBuilder;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Tag;
import org.xwiki.rest.model.jaxb.Tags;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.tag.TagPlugin;

@Component("org.xwiki.rest.resources.tags.TagsResource")
@Path("/wikis/{wikiName}/tags")
public class TagsResource extends XWikiResource
{
    @GET
    public Tags getTags(@PathParam("wikiName") String wikiName) throws XWikiException
    {
        String database = xwikiContext.getDatabase();

        Tags tags = objectFactory.createTags();

        /* This try is just needed for executing the finally clause. */
        try {
            xwikiContext.setDatabase(wikiName);

            TagPlugin tagPlugin = (TagPlugin) xwiki.getPlugin("tag", xwikiContext);

            List<String> tagNames = tagPlugin.getAllTags(xwikiContext);

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
            xwikiContext.setDatabase(database);
        }

        return tags;
    }
}
