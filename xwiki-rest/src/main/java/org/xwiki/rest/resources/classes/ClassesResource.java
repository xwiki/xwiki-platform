package org.xwiki.rest.resources.classes;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.RangeIterable;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Classes;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/classes")
public class ClassesResource extends XWikiResource /* extends XWikiResource */
{

    public ClassesResource(@Context UriInfo uriInfo)
    {
        super(uriInfo);
    }

    @GET
    public Classes getClasses(@PathParam("wikiName") String wikiName,
        @QueryParam("start") @DefaultValue("0") Integer start, @QueryParam("number") @DefaultValue("-1") Integer number)
        throws XWikiException
    {

        String database = xwikiContext.getDatabase();

        try {
            xwikiContext.setDatabase(wikiName);

            List<String> classNames = xwikiApi.getClassList();
            Collections.sort(classNames);

            RangeIterable<String> ri = new RangeIterable<String>(classNames, start, number);

            Classes classes = objectFactory.createClasses();

            for (String className : ri) {
                com.xpn.xwiki.api.Class xwikiClass = xwikiApi.getClass(className);
                classes.getClazzs().add(
                    DomainObjectFactory.createClass(objectFactory, uriInfo.getBaseUri(), wikiName, xwikiClass));
            }

            return classes;
        } finally {
            xwiki.setDatabase(database);
        }
    }
}
