package org.xwiki.rest.resources.classes;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Class;
import org.xwiki.rest.model.jaxb.Property;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/classes/{className}/properties/{propertyName}")
public class ClassPropertyResource extends XWikiResource
{

    public ClassPropertyResource(@Context UriInfo uriInfo)
    {
        super(uriInfo);
    }

    @GET
    public Property getClassProperty(@PathParam("wikiName") String wikiName, @PathParam("className") String className, @PathParam("propertyName") String propertyName)
        throws XWikiException
    {

        String database = xwikiContext.getDatabase();

        try {
            xwikiContext.setDatabase(wikiName);

            com.xpn.xwiki.api.Class xwikiClass = xwikiApi.getClass(className);
            if(xwikiClass == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }
                        
            Class clazz = DomainObjectFactory.createClass(objectFactory, uriInfo.getBaseUri(), wikiName, xwikiClass);
            
            for (Property property : clazz.getProperties()) {
                if (property.getName().equals(propertyName)) {
                    return property;
                }
            }

            throw new WebApplicationException(Status.NOT_FOUND);            
            
        } finally {
            xwiki.setDatabase(database);
        }
    }
}
