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
package org.xwiki.container;

import org.xwiki.component.annotation.Role;

/**
 * Provides access to the components representing the current execution context (request, response and session).
 *
 * @version $Id$
 */
@Role
public interface Container
{
    /**
     * @return the application context
     * @deprecated use the notion of {@link org.xwiki.environment.Environment} instead
     */
    @Deprecated(since = "3.5M1")
    ApplicationContext getApplicationContext();

    /**
     * @param context the application context to set
     * @deprecated use the notion of {@link org.xwiki.environment.Environment} instead
     */
    @Deprecated(since = "3.5M1")
    void setApplicationContext(ApplicationContext context);

    /**
     * @return the current request
     */
    Request getRequest();

    /**
     * @param request the request to set as the current one
     */
    void setRequest(Request request);

    /**
     * Remove the current request.
     */
    void removeRequest();

    /**
     * @param request the request to push as the current one, keeping the previous one on the stack
     */
    void pushRequest(Request request);

    /**
     * Restore the previously pushed request as the current one.
     */
    void popRequest();

    /**
     * @return the current response
     */
    Response getResponse();

    /**
     * @param response the response to set as the current one
     */
    void setResponse(Response response);

    /**
     * Remove the current response.
     */
    void removeResponse();

    /**
     * @param response the response to push as the current one, keeping the previous one on the stack
     */
    void pushResponse(Response response);

    /**
     * Restore the previously pushed response as the current one.
     */
    void popResponse();

    /**
     * @return the current session
     */
    Session getSession();

    /**
     * @param session the session to set as the current one
     */
    void setSession(Session session);

    /**
     * Remove the current session.
     */
    void removeSession();

    /**
     * @param session the session to push as the current one, keeping the previous one on the stack
     */
    void pushSession(Session session);

    /**
     * Restore the previously pushed session as the current one.
     */
    void popSession();
}
