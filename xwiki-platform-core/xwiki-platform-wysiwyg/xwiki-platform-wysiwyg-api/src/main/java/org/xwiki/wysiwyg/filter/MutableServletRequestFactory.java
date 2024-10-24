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
package org.xwiki.wysiwyg.filter;

import org.xwiki.component.annotation.Role;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;
import org.xwiki.stability.Unstable;
import org.xwiki.wysiwyg.internal.filter.http.JakartaToJavaxMutableHttpServletRequest;
import org.xwiki.wysiwyg.internal.filter.http.JavaxToJakartaMutableHttpServletRequest;

import jakarta.servlet.ServletRequest;

/**
 * A factory for mutable servlet requests. This factory is needed because concrete mutable servlet requests don't have a
 * default constructor and I couldn't make the component manager (Plexus) inject the current servlet request when
 * instantiating mutable servlet requets.
 * 
 * @version $Id$
 */
@Role
public interface MutableServletRequestFactory
{
    /**
     * Creates a new mutable servlet request.
     * 
     * @param request The original servlet request to wrap.
     * @return a new mutable servlet request.
     * @deprecated use {@link #newInstance(ServletRequest)} instead
     */
    @Deprecated(since = "42.0.0")
    default MutableServletRequest newInstance(javax.servlet.ServletRequest request)
    {
        return new JavaxToJakartaMutableHttpServletRequest(newInstance(JakartaServletBridge.toJakarta(request)));
    }

    /**
     * Creates a new mutable servlet request.
     * 
     * @param request The original servlet request to wrap.
     * @return a new mutable servlet request.
     * @since 42.0.0
     */
    @Unstable
    default MutableJakartaServletRequest newInstance(ServletRequest request)
    {
        return new JakartaToJavaxMutableHttpServletRequest(newInstance(JakartaServletBridge.toJavax(request)));
    }
}
