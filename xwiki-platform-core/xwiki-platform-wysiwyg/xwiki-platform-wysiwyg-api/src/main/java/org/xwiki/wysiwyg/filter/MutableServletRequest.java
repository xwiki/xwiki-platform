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

import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * A servlet request that can be modified. It is very useful, for instance, when you need to change the values of some
 * request parameters, inside a filter.
 * 
 * @version $Id$
 * @deprecated use {@link MutableJakartaServletRequest} instead
 */
@Deprecated(since = "42.0.0")
public interface MutableServletRequest extends ServletRequest
{
    /**
     * Sets the value of a request parameter.
     * 
     * @param name the name of the request parameter
     * @param value the new value of the request parameter
     * @return the old value of the specified request parameter, or {@code null} if this is the first time we set its
     *         value
     */
    String setParameter(String name, String value);

    /**
     * Sets the values of a request parameter.
     * 
     * @param name the name of the request parameter
     * @param values the new array of values for the specified request parameter
     * @return the old values of the specified request parameter, or {@code null} if this is the first time we set its
     *         values
     */
    String[] setParameterValues(String name, String[] values);

    /**
     * Removes the request parameter with the specified name.
     * 
     * @param name a string representing the name of the request parameter to be removed
     * @return the old value of the specified request parameter, or {@code null} if it wasn't set
     */
    String removeParameter(String name);

    /**
     * Redirects this request to the specified URL. We had to add this method since there's no generic way to redirect a
     * {@link ServletRequest}.
     * 
     * @param response the response object used to redirect
     * @param url the location where to redirect
     * @throws IOException if the redirect fails
     */
    void sendRedirect(ServletResponse response, String url) throws IOException;

    /**
     * @return the URL of the requester
     */
    String getReferer();

    /**
     * @param attrName the name of the session attribute whose value should be retrieved
     * @return the value of the specified session attribute
     */
    Object getSessionAttribute(String attrName);

    /**
     * Sets the value of a session attribute.
     * 
     * @param attrName the name of the session attribute
     * @param attrValue the value to be set
     * @return the previous value of the specified session attribute
     */
    Object setSessionAttribute(String attrName, Object attrValue);

    /**
     * @return the request object wrapped by this object
     */
    ServletRequest getRequest();
}
