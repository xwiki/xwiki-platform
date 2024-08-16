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
package org.xwiki.container.script;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.Response;
import org.xwiki.container.Session;
import org.xwiki.container.internal.script.ScriptRequest;
import org.xwiki.container.internal.script.ScriptResponse;
import org.xwiki.container.internal.script.ScriptSession;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.stability.Unstable;

import jakarta.inject.Inject;

/**
 * The main entry point for a script to manipulate {@link Container} related APIs.
 * 
 * @version $Id$
 * @since 42.0.0
 */
@Unstable
@Component
@Singleton
public class ContainerScriptService
{
    @Inject
    private Container container;

    @Inject
    private ContextualAuthorizationManager authorization;

    /**
     * @return the current request
     */
    public Request getRequest()
    {
        return new ScriptRequest(this.container.getRequest(), this.authorization);
    }

    /**
     * @return the current response
     */
    public Response getResponse()
    {
        return new ScriptResponse(this.container.getResponse(), this.authorization);
    }

    /**
     * @return the current session
     */
    public Session getSession()
    {
        return new ScriptSession(this.container.getSession(), this.authorization);
    }
}
