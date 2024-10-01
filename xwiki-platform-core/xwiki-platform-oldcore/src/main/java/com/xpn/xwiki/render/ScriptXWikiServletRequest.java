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

import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.web.WrappingXWikiRequest;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequest;

/**
 * A wrapper around {@link XWikiRequest} with security related checks.
 * 
 * @version $Id$
 * @since 12.3RC1
 * @since 12.2.1
 * @since 11.10.5
 * @deprecated use the container script service instead
 */
//TODO: uncomment the annotation when XWiki Standard scripts are fully migrated to the new API
//@Deprecated(since = "42.0.0")
public class ScriptXWikiServletRequest extends WrappingXWikiRequest
{
    /**
     * Set of request attributes that require programming rights to be modified.
     */
    private static final Set<String> READ_ONLY_ATTRIBUTES = Set.of(XWikiServletRequest.ATTRIBUTE_EFFECTIVE_AUTHOR);

    private final ContextualAuthorizationManager authorization;

    /**
     * @param request the wrapped request
     * @param authorization used to check rights of the current author
     */
    public ScriptXWikiServletRequest(XWikiRequest request, ContextualAuthorizationManager authorization)
    {
        super(request);

        this.authorization = authorization;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Only allowed to author with programming right because it contains very sensitive data.
     * 
     * @see javax.servlet.ServletRequestWrapper#getServletContext()
     */
    @Override
    public ServletContext getServletContext()
    {
        if (this.authorization.hasAccess(Right.PROGRAM)) {
            return super.getServletContext();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Only allowed to author with programming right because it contains very sensitive data.
     * 
     * @see com.xpn.xwiki.web.XWikiServletRequest#getHttpServletRequest()
     */
    @Override
    public HttpServletRequest getHttpServletRequest()
    {
        if (this.authorization.hasAccess(Right.PROGRAM)) {
            return super.getHttpServletRequest();
        }

        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Return a protected version of the session.
     * 
     * @see javax.servlet.http.HttpServletRequestWrapper#getSession()
     */
    @Override
    public HttpSession getSession()
    {
        return new ScriptHttpSession(super.getSession(), this.authorization);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Return a protected version of the session.
     * 
     * @see javax.servlet.http.HttpServletRequestWrapper#getSession(boolean)
     */
    @Override
    public HttpSession getSession(boolean create)
    {
        return new ScriptHttpSession(super.getSession(create), this.authorization);
    }

    @Override
    public void setAttribute(String name, Object value)
    {
        if (!READ_ONLY_ATTRIBUTES.contains(name) || this.authorization.hasAccess(Right.PROGRAM)) {
            super.setAttribute(name, value);
        }
    }
}
