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
package com.xpn.xwiki.render;

import org.xwiki.jakartabridge.servlet.internal.JakartaToJavaxHttpServletResponse;
import org.xwiki.security.authorization.ContextualAuthorizationManager;

import com.xpn.xwiki.web.WrappingXWikiResponse;
import com.xpn.xwiki.web.XWikiResponse;

/**
 * A wrapper around {@link XWikiResponse} for scripts.
 * 
 * @version $Id$
 * @since 17.0.0RC1
 * @deprecated use the {@link org.xwiki.container.script.ContainerScriptService} instead
 */
// TODO: uncomment the annotation when XWiki Standard scripts are fully migrated to the new API
// @Deprecated(since = "17.0.0RC1")
public class ScriptXWikiServletResponse extends WrappingXWikiResponse
{
    private final ContextualAuthorizationManager authorization;

    /**
     * @param response the wrapped response
     * @deprecated use {@link #ScriptXWikiServletResponse(XWikiResponse, ContextualAuthorizationManager)} instead
     */
    @Deprecated(since = "18.4.0RC1, 17.10.9")
    public ScriptXWikiServletResponse(XWikiResponse response)
    {
        this(response, null);
    }

    /**
     * @param response the wrapped response
     * @param authorization used to check rights of the current author
     * @since 18.4.0RC1
     * @since 17.10.9
     */
    public ScriptXWikiServletResponse(XWikiResponse response, ContextualAuthorizationManager authorization)
    {
        super(response);

        this.authorization = authorization;
    }

    @Override
    public jakarta.servlet.http.HttpServletResponse getJakarta()
    {
        return new JakartaToJavaxHttpServletResponse<>(this);
    }
}
