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

import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.xwiki.rest.resources.BrowserAuthenticationResource;

/**
 * @version $Id$
 */
public class XWikiJaxRsApplication extends JaxRsApplication
{
    @Override
    public Restlet createRoot()
    {
        XWikiSetupCleanupFilter setupCleanupFilter = new XWikiSetupCleanupFilter();
        XWikiAuthentication xwikiAuthentication = new XWikiAuthentication(getContext());

        Router router = new Router();
        router.attach(BrowserAuthenticationResource.URI_PATTERN, BrowserAuthenticationResource.class);
        Restlet root = super.createRoot();
        router.attach(root);
        setupCleanupFilter.setNext(xwikiAuthentication);
        xwikiAuthentication.setNext(router);

        return setupCleanupFilter;
    }

}
