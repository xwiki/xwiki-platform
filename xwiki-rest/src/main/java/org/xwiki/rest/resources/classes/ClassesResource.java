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
import org.xwiki.rest.model.Space;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

public class ClassesResource extends XWikiResource
{
    @Override
    public Representation represent(Variant variant)
    {
        String database = xwikiContext.getDatabase();

        try {
            String wiki = (String) getRequest().getAttributes().get(Constants.WIKI_NAME_PARAMETER);
            xwikiContext.setDatabase(wiki);

            List<String> classNames = xwikiApi.getClassList();
            Collections.sort(classNames);

            Form queryForm = getRequest().getResourceRef().getQueryAsForm();
            RangeIterable<String> ri =
                new RangeIterable<String>(classNames, Utils.parseInt(
                    queryForm.getFirstValue(Constants.START_PARAMETER), 0), Utils.parseInt(queryForm
                    .getFirstValue(Constants.NUMBER_PARAMETER), -1));

            Classes classes = new Classes();

            for (String className : ri) {
                com.xpn.xwiki.api.Class xwikiClass = xwikiApi.getClass(className);
                classes
                    .addClass(DomainObjectFactory.createSpace(getRequest(), resourceClassRegistry, wiki, xwikiClass));
            }

            return getRepresenterFor(variant).represent(getContext(), getRequest(), getResponse(), classes);
        } catch (XWikiException e) {
            e.printStackTrace();
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        } finally {
            xwiki.setDatabase(database);
        }

        return null;
    }
}
