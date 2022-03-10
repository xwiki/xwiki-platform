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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * A wrapper around {@link HttpSession} with security related checks.
 * 
 * @version $Id$
 * @since 12.4RC1
 * @since 11.10.5
 */
public class ScriptHttpSession implements HttpSession, HttpSessionContext
{
    private static final String KEY_SAFESESSION = ScriptHttpSession.class.getName();

    private final HttpSession session;

    private final ContextualAuthorizationManager authorization;

    /**
     * @param session the wrapped session
     * @param authorization used to check rights of the current author
     */
    public ScriptHttpSession(HttpSession session, ContextualAuthorizationManager authorization)
    {
        this.session = session;
        this.authorization = authorization;
    }

    @Override
    public long getCreationTime()
    {
        return this.session.getCreationTime();
    }

    @Override
    public String getId()
    {
        return this.session.getId();
    }

    @Override
    public long getLastAccessedTime()
    {
        return this.session.getLastAccessedTime();
    }

    @Override
    public ServletContext getServletContext()
    {
        if (this.authorization.hasAccess(Right.PROGRAM)) {
            return this.session.getServletContext();
        }

        return null;
    }

    @Override
    public void setMaxInactiveInterval(int interval)
    {
        this.session.setMaxInactiveInterval(interval);
    }

    @Override
    public int getMaxInactiveInterval()
    {
        return this.session.getMaxInactiveInterval();
    }

    @Override
    public HttpSessionContext getSessionContext()
    {
        return this;
    }

    /**
     * Access an attribute that is safe to use for any script author.
     * 
     * @param name the name of the attribute
     * @return the value of the attribute
     */
    public Object getSafeAttribute(String name)
    {
        Map<String, Object> safeSession = (Map<String, Object>) this.session.getAttribute(KEY_SAFESESSION);

        return safeSession != null ? safeSession.get(name) : null;
    }

    /**
     * Set an attribute that is safe to use for any script author.
     * <p>
     * It's recommended to not store anything sensitive in there.
     * 
     * @param name the name of the attribute
     * @param value the value of the attribute
     */
    public void setSafeAttribute(String name, Object value)
    {
        Map<String, Object> safeSession = (Map<String, Object>) this.session.getAttribute(KEY_SAFESESSION);

        if (safeSession == null) {
            safeSession = new ConcurrentHashMap<>();
            this.session.setAttribute(KEY_SAFESESSION, safeSession);
        }

        safeSession.put(name, value);
    }

    /**
     * Remove an attribute that is safe to use for any script author.
     * 
     * @param name the name of the attribute
     */
    public void removeSafeAttribute(String name)
    {
        Map<String, Object> safeSession = (Map<String, Object>) this.session.getAttribute(KEY_SAFESESSION);

        if (safeSession != null) {
            safeSession.remove(name);
        }
    }

    /**
     * @return the names of the attributes which are safe to use for any script author.
     */
    public Enumeration<String> getSafeAttributeNames()
    {
        Map<String, Object> safeSession = (Map<String, Object>) this.session.getAttribute(KEY_SAFESESSION);

        return safeSession != null ? Collections.enumeration(safeSession.keySet()) : Collections.emptyEnumeration();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Allow to manipulate only a limited set of attributes when not a programming right user since other might be
     * sensitive data.
     * 
     * @see javax.servlet.http.HttpSession#getAttribute(java.lang.String)
     */
    @Override
    public Object getAttribute(String name)
    {
        return this.authorization.hasAccess(Right.PROGRAM) ? this.session.getAttribute(name) : getSafeAttribute(name);
    }

    @Override
    public Object getValue(String name)
    {
        return getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames()
    {
        return this.authorization.hasAccess(Right.PROGRAM) ? this.session.getAttributeNames() : getSafeAttributeNames();
    }

    @Override
    public String[] getValueNames()
    {
        return this.session.getValueNames();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Allow to manipulate only a limited set of attributes when not a programming right user since other might be
     * sensitive data.
     * 
     * @see javax.servlet.http.HttpSession#setAttribute(java.lang.String, java.lang.Object)
     */
    @Override
    public void setAttribute(String name, Object value)
    {
        if (this.authorization.hasAccess(Right.PROGRAM)) {
            this.session.setAttribute(name, value);
        } else {
            setSafeAttribute(name, value);
        }
    }

    @Override
    public void putValue(String name, Object value)
    {
        setAttribute(name, value);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Allow to manipulate only a limited set of attributes when not a programming right user since other might be
     * sensitive data.
     * 
     * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
     */
    @Override
    public void removeAttribute(String name)
    {
        if (this.authorization.hasAccess(Right.PROGRAM)) {
            this.session.removeAttribute(name);
        } else {
            removeSafeAttribute(name);
        }
    }

    @Override
    public void removeValue(String name)
    {
        removeAttribute(name);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Allow to manipulate only a limited set of attributes when not a programming right user since other might be
     * sensitive data.
     * 
     * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
     */
    @Override
    public void invalidate()
    {
        if (this.authorization.hasAccess(Right.PROGRAM)) {
            this.session.invalidate();
        }
    }

    @Override
    public boolean isNew()
    {
        return this.session.isNew();
    }

    // HttpSessionContext

    @Override
    public HttpSession getSession(String sessionId)
    {
        return this;
    }

    @Override
    public Enumeration<String> getIds()
    {
        return this.session.getSessionContext().getIds();
    }
}
