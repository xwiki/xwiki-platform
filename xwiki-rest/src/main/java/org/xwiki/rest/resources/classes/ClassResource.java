package org.xwiki.rest.resources.classes;

import java.util.Collections;
import java.util.List;

import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xwiki.rest.Constants;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.RangeIterable;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.Classes;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Class;
import com.xpn.xwiki.api.Document;

public class ClassResource extends XWikiResource
{
    @Override
    public Representation represent(Variant variant)
    {
        String database = xwikiContext.getDatabase();

        try {
            String wiki = (String) getRequest().getAttributes().get(Constants.WIKI_NAME_PARAMETER);
            String className = (String) getRequest().getAttributes().get(Constants.CLASS_NAME_PARAMETER);
            xwikiContext.setDatabase(wiki);

            Class xwikiClass = xwikiApi.getClass(className);
            if (xwikiClass == null) {
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return null;
            }

            org.xwiki.rest.model.Class theClass =
                DomainObjectFactory.createSpace(getRequest(), resourceClassRegistry, wiki, xwikiClass);

            return getRepresenterFor(variant).represent(getContext(), getRequest(), getResponse(), theClass);
        } catch (XWikiException e) {
            e.printStackTrace();
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        } finally {
            xwiki.setDatabase(database);
        }

        return null;
    }
}
