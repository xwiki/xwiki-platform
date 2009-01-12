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

import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.service.StatusService;

/**
 * A status service that is able to catch unhandled exceptions and correctly release the resource that was serving the
 * request.
 * 
 * @version $Id$
 */
public class XWikiStatusService extends StatusService
{
    private XWikiRestApplication application;

    public XWikiStatusService(XWikiRestApplication application)
    {
        super();
        this.application = application;
    }

    @Override
    public Status getStatus(Throwable throwable, Request request, Response response)
    {
        Utils.cleanupResource(request, application.getComponentManager(), application.getLogger());

        return super.getStatus(throwable, request, response);
    }

}
