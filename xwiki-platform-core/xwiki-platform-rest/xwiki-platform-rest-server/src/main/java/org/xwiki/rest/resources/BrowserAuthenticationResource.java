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
package org.xwiki.rest.resources;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.representation.Variant;
import org.restlet.resource.ServerResource;

/**
 * @version $Id$
 */
public class BrowserAuthenticationResource extends ServerResource
{
    public static final String URI_PATTERN = "/browser_authentication";

    public BrowserAuthenticationResource(Context context, Request request, Response response)
    {
        super();
        this.init(context, request, response);
        getVariants().clear();
        getVariants().add(new Variant(MediaType.TEXT_PLAIN));
    }

    
    public void handleGet()
    {
        getResponse().redirectSeeOther(String.format("%s/", getRequest().getRootRef()));
    }

}
