package org.xwiki.rest.resources.tags;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.RangeIterable;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Pages;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.plugin.tag.TagPlugin;

@Component("org.xwiki.rest.resources.tags.PagesForTagsResource")
@Path("/wikis/{wikiName}/tags/{tagNames}")
public class PagesForTagsResource extends XWikiResource
{
    @GET
    public Pages getTags(@PathParam("wikiName") String wikiName, @PathParam("tagNames") String tagNames,
        @QueryParam("start") @DefaultValue("0") Integer start, @QueryParam("number") @DefaultValue("-1") Integer number)
        throws XWikiException
    {
        String database = xwikiContext.getDatabase();

        Pages pages = objectFactory.createPages();

        /* This try is just needed for executing the finally clause. */
        try {
            xwikiContext.setDatabase(wikiName);

            TagPlugin tagPlugin = (TagPlugin) xwiki.getPlugin("tag", xwikiContext);

            String[] tagNamesArray = tagNames.split(",");

            List<String> documentNames = new ArrayList<String>();
            for (String tagName : tagNamesArray) {
                List<String> documentNamesForTag = tagPlugin.getDocumentsWithTag(tagName, xwikiContext);

                /* Avoid duplicates */
                for (String documentName : documentNamesForTag) {
                    if (!documentNames.contains(documentName)) {
                        documentNames.add(documentName);
                    }
                }
            }

            RangeIterable<String> ri = new RangeIterable<String>(documentNames, start, number);

            for (String documentName : ri) {
                Document doc = xwikiApi.getDocument(documentName);
                if (doc != null) {
                    pages.getPageSummaries().add(
                        DomainObjectFactory.createPageSummary(objectFactory, uriInfo.getBaseUri(), doc));
                }
            }

        } finally {
            xwikiContext.setDatabase(database);
        }

        return pages;
    }
}
