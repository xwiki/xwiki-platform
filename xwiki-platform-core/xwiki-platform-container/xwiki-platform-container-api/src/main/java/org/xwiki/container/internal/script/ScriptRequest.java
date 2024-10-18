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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.xwiki.container.Request;
import org.xwiki.container.wrap.WrappingRequest;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * A wrapper around {@link Request} with security related checks.
 * 
 * @version $Id$
 * @since 42.0.0
 */
public class ScriptRequest extends WrappingRequest
{
    /**
     * Set of request attributes that require programming rights to be modified.
     */
    private static final Set<String> READ_ONLY_ATTRIBUTES = Set.of(Request.ATTRIBUTE_EFFECTIVE_AUTHOR);

    private static final String KEY_SAFEATTRIBUTES = ScriptRequest.class.getName() + "#attributes";

    private final ContextualAuthorizationManager authorization;

    /**
     * @param request the wrapped request
     * @param authorization used to check rights of the current author
     */
    public ScriptRequest(Request request, ContextualAuthorizationManager authorization)
    {
        super(request);

        this.authorization = authorization;
    }

    @Override
    public Request getRequest()
    {
        return this.authorization.hasAccess(Right.PROGRAM) ? super.getRequest() : null;
    }

    /**
     * Access an attribute that is safe to use for any script author.
     * 
     * @param name the name of the attribute
     * @return the value of the attribute
     */
    public Object getSafeAttribute(String name)
    {
        Map<String, Object> safeAttributes = (Map<String, Object>) this.request.getAttribute(KEY_SAFEATTRIBUTES);

        return safeAttributes != null ? safeAttributes.get(name) : null;
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
        if (!READ_ONLY_ATTRIBUTES.contains(name) || this.authorization.hasAccess(Right.PROGRAM)) {
            Map<String, Object> safeSession = (Map<String, Object>) this.request.getAttribute(KEY_SAFEATTRIBUTES);

            if (safeSession == null) {
                safeSession = new ConcurrentHashMap<>();
                this.request.setAttribute(KEY_SAFEATTRIBUTES, safeSession);
            }

            safeSession.put(name, value);
        }
    }

    /**
     * Remove an attribute that is safe to use for any script author.
     * 
     * @param name the name of the attribute
     */
    public void removeSafeAttribute(String name)
    {
        if (!READ_ONLY_ATTRIBUTES.contains(name) || this.authorization.hasAccess(Right.PROGRAM)) {
            Map<String, Object> safeSession = (Map<String, Object>) this.request.getAttribute(KEY_SAFEATTRIBUTES);

            if (safeSession != null) {
                safeSession.remove(name);
            }
        }
    }

    /**
     * @return the names of the attributes which are safe to use for any script author.
     */
    public Enumeration<String> getSafeAttributeNames()
    {
        Map<String, Object> safeSession = (Map<String, Object>) this.request.getAttribute(KEY_SAFEATTRIBUTES);

        return safeSession != null ? Collections.enumeration(safeSession.keySet()) : Collections.emptyEnumeration();
    }

    @Override
    public Object getAttribute(String name)
    {
        return this.authorization.hasAccess(Right.PROGRAM) ? this.request.getAttribute(name) : getSafeAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames()
    {
        return this.authorization.hasAccess(Right.PROGRAM) ? this.request.getAttributeNames() : getSafeAttributeNames();
    }

    @Override
    public void setAttribute(String name, Object value)
    {
        if (this.authorization.hasAccess(Right.PROGRAM)) {
            this.request.setAttribute(name, value);
        } else {
            setSafeAttribute(name, value);
        }
    }

    @Override
    public void removeAttribute(String name)
    {
        if (this.authorization.hasAccess(Right.PROGRAM)) {
            this.request.removeAttribute(name);
        } else {
            removeSafeAttribute(name);
        }
    }
}
