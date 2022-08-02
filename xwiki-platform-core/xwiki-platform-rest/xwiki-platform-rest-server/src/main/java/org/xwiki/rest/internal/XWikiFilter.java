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
package org.xwiki.rest.internal;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.routing.Filter;
import org.restlet.util.Series;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;

/**
 * The filter is called before serving any request and it is responsible to set in user in the response header. The user
 * is expected to be authenticate in a preceding filter along with the XWikiContext initialization.
 * 
 * @version $Id$
 * @since 13.4RC1
 */
public class XWikiFilter extends Filter
{
    /**
     * Constructor.
     * 
     * @param context The context.
     */
    public XWikiFilter(Context context) throws IllegalArgumentException
    {
        super(context);
    }

    @Override
    protected int beforeHandle(Request request, Response response)
    {
        ComponentManager componentManager =
            (ComponentManager) getContext().getAttributes().get(Constants.XWIKI_COMPONENT_MANAGER);
        XWikiContext xwikiContext = Utils.getXWikiContext(componentManager);

        try {
            EntityReferenceSerializer<String> serializer =
                componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING);

            /*
             * We add headers to the response to allow applications to verify if the authentication is still valid. We
             * are also adding the XWiki version at the same moment.
             */
            Series<Header> responseHeaders =
                (Series<Header>) response.getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
            if (responseHeaders == null) {
                responseHeaders = new Series<>(Header.class);
                response.getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, responseHeaders);
            }
            responseHeaders.add("XWiki-User", serializer.serialize(xwikiContext.getUserReference()));
            responseHeaders.add("XWiki-Version", xwikiContext.getWiki().getVersion());
        } catch (ComponentLookupException e) {
            getLogger()
                .warning("Failed to lookup the entity reference serializer: " + ExceptionUtils.getRootCauseMessage(e));
        }

        return CONTINUE;
    }
}
