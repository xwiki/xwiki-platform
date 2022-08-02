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

@Role
public interface Container
{
    /**
     * @deprecated use the notion of {@link org.xwiki.environment.Environment} instead
     */
    @Deprecated(since = "3.5M1")
    ApplicationContext getApplicationContext();

    /**
     * @deprecated use the notion of {@link org.xwiki.environment.Environment} instead
     */
    @Deprecated(since = "3.5M1")
    void setApplicationContext(ApplicationContext context);
    
    Request getRequest();
    void setRequest(Request request);
    void removeRequest();
    void pushRequest(Request request);
    void popRequest();

    Response getResponse();
    void setResponse(Response response);
    void removeResponse();
    void pushResponse(Response response);
    void popResponse();

    Session getSession();
    void setSession(Session session);
    void removeSession();
    void pushSession(Session session);
    void popSession();
}
