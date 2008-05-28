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
 *
 */
package org.xwiki.container.daemon;

public interface DaemonContainerInitializer
{
    /**
     * This component's role, used when code needs to look it up.
     */
    String ROLE = DaemonContainerInitializer.class.getName();

    /**
     * Automatically initializes the Container component with a new Daemon Request.
     *
     * @param xwikiContext the XWiki Context to save in the Request. Note that this is a bridge with the old
     *        architecture so that new components can access the old XWiki Context. This will disappear when all old
     *        code will have been moved to components.
     * @throws DaemonContainerException if one of the {@link org.xwiki.container.RequestInitializer} fails to
     *         initialize properly
     */
    void initializeRequest(Object xwikiContext) throws DaemonContainerException;

    /**
     * Automatically initializes the Container component with a new Daemon Request which is pushed on top of the
     * current Request. It's expected that the request will be popped later on so that the previous Request is
     * made active again.
     *
     * @param xwikiContext the XWiki Context to save in the Request. Note that this is a bridge with the old
     *        architecture so that new components can access the old XWiki Context. This will disappear when all old
     *        code will have been moved to components.
     * @throws DaemonContainerException if one of the {@link org.xwiki.container.RequestInitializer} fails to
     *         initialize properly
     */
    void pushRequest(Object xwikiContext)throws DaemonContainerException;
}
