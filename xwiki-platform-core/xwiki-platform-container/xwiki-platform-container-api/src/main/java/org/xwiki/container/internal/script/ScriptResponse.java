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
package org.xwiki.container.internal.script;

import org.xwiki.container.Response;
import org.xwiki.container.wrap.WrappingResponse;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * A wrapper around {@link Response} with security related checks.
 * 
 * @version $Id$
 * @since 42.0.0
 */
public class ScriptResponse extends WrappingResponse
{
    private final ContextualAuthorizationManager authorization;

    /**
     * @param response the wrapped response
     * @param authorization used to check rights of the current author
     */
    public ScriptResponse(Response response, ContextualAuthorizationManager authorization)
    {
        super(response);

        this.authorization = authorization;
    }

    @Override
    public Response getResponse()
    {
        return this.authorization.hasAccess(Right.PROGRAM) ? super.getResponse() : null;
    }
}
