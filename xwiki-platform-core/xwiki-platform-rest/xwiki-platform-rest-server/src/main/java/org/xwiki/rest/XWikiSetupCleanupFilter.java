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
package org.xwiki.rest;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.routing.Filter;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

/**
 * <p>
 * The Setup cleanup filter is used to populate the Restlet context with relevant variables that are used by JAX-RS
 * resources. It is also used to release the JAX-RS resources that are instantiated by using the component manager, in
 * order to avoid memory leaks.
 * </p>
 * 
 * @version $Id$
 */
public class XWikiSetupCleanupFilter extends Filter
{
    @Override
    protected int beforeHandle(Request request, Response response)
    {
        /*
         * We put the original HTTP request in context attributes because this is needed for reading
         * application/www-form-urlencoded POSTs. In fact servlet filters might call getParameters() which invalidates
         * the request body, making Restlet unable to process it. In this case we need to use getParameters as well
         * instead of reading form data from the input stream, and in order to do this we need the original HTTP request
         * object. This is basically a hack that should be removed as soon as the Restlet JAX-RS extension will support
         * the injection of the request object via the @Context annotation.
         */
        getContext().getAttributes().put(Constants.HTTP_REQUEST, getHttpRequest(request));

        return Filter.CONTINUE;
    }

    @Override
    protected void afterHandle(Request request, Response response)
    {
        // Release all the JAX-RS resources that are implemented as components with per-lookup policy and that have been
        // instantiated during this request.
        ComponentManager componentManager =
            (ComponentManager) getContext().getAttributes().get(Constants.XWIKI_COMPONENT_MANAGER);
        for (XWikiRestComponent component : getReleasableComponents(componentManager)) {
            try {
                componentManager.release(component);
            } catch (ComponentLifecycleException e) {
                getLogger().log(Level.WARNING, "Unable to release component [{0}]. ({1})",
                    new Object[] {component.getClass().getName(), e.getMessage()});
            }
        }

        /* Avoid that empty entities make the engine forward the response creation to the XWiki servlet. */
        if (response.getEntity() != null) {
            if (!response.getEntity().isAvailable()) {
                response.setEntity(null);
            }
        }
    }

    /**
     * @param componentManager the component manager
     * @return the list of JAX-RS resources that are implemented as components with per-lookup policy and that have been
     *         instantiated during this request
     */
    private List<XWikiRestComponent> getReleasableComponents(ComponentManager componentManager)
    {
        try {
            ExecutionContext executionContext = componentManager.<Execution> getInstance(Execution.class).getContext();
            @SuppressWarnings("unchecked")
            List<XWikiRestComponent> releasableComponents =
                (List<XWikiRestComponent>) executionContext.getProperty(Constants.RELEASABLE_COMPONENT_REFERENCES);
            return releasableComponents != null ? releasableComponents : Collections.<XWikiRestComponent> emptyList();
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to retrieve the list of releasable components.", e);
            return Collections.emptyList();
        }
    }

    /**
     * <p>
     * Retrieves the original servlet request.
     * </p>
     * 
     * @param req The Restlet request to handle.
     * @return httpServletRequest The original HTTP servlet request.
     */
    protected static HttpServletRequest getHttpRequest(Request req)
    {
        return ServletUtils.getRequest(req);
    }

}
