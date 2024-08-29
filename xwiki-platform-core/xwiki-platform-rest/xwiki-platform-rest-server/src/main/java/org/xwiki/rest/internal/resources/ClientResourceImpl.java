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
package org.xwiki.rest.internal.resources;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Client;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.resources.ClientResource;
import org.xwiki.rest.resources.RootResource;

import com.xpn.xwiki.user.impl.xwiki.MyPersistentLoginManager;

/**
 * Default implementation of {@link ClientResource}.
 * 
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.ClientResourceImpl")
public class ClientResourceImpl extends XWikiResource implements ClientResource
{
    @Override
    public Client getClient()
    {
        String parentUri = Utils.createURI(this.uriInfo.getBaseUri(), RootResource.class).toString();
        Link parentLink = objectFactory.createLink();
        parentLink.setHref(parentUri);
        parentLink.setRel(Relations.PARENT);

        Link selfLink = this.objectFactory.createLink();
        selfLink.setHref(this.uriInfo.getAbsolutePath().toString());
        selfLink.setRel(Relations.SELF);

        return new Client().withIp(getClientIP()).withLinks(parentLink, selfLink);
    }

    private String getClientIP()
    {
        // TODO: Either make MyPersistentLoginManager#getClientIP() and use the dedicated provider to get an instance,
        // or extract the code from MyPersistentLoginManager#getClientIP() in a separate component.
        return new MyPersistentLoginManager()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public String getClientIP(HttpServletRequest request)
            {
                return super.getClientIP(request);
            }
        }.getClientIP(getXWikiContext().getRequest());
    }
}
