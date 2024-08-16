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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Represent metadata associated with a request sent to an XWiki container by a client.
 * <p>
 * The difference between properties and attributes is that the properties are expected to be sent by the client while
 * attributes are generally set on server side during the request execution by other components.
 * 
 * @version $Id$
 * @since 1.2M1
 */
public interface Request
{
    /**
     * The name of the attribute holding the request effective author.
     * 
     * @since 42.0.0
     */
    @Unstable
    public static final String ATTRIBUTE_EFFECTIVE_AUTHOR = Request.class.getName() + "#effectiveAuthor";

    /**
     * Returns the value of a request parameter as a <code>String</code>, or <code>null</code> if the parameter does not
     * exist. Request parameters are extra information sent with the request. For HTTP servlets, parameters are
     * contained in the query string or posted form data.
     * <p>
     * You should only use this method when you are sure the parameter has only one value. If the parameter might have
     * more than one value, use {@link #getProperties}.
     * <p>
     * If you use this method with a multivalued parameter, the value returned is equal to the first value in the array
     * returned by <code>getParameterValues</code>.
     * 
     * @param key a <code>String</code> specifying the name of the parameter
     * @return an <code>Object</code> containing the value of the parameter, or <code>null</code> if the parameter does
     *         not exist
     * @see #getParameterValues
     * @since 42.0.0
     */
    @Unstable
    default Object getParameter(String key)
    {
        return null;
    }

    /**
     * Returns an array of <code>String</code> objects containing all of the values the given request parameter has, or
     * <code>null</code> if the parameter does not exist.
     * <p>
     * If the parameter has a single value, the array has a length of 1.
     *
     * @param name a <code>String</code> containing the name of the parameter whose value is requested
     * @return an array of <code>String</code> objects containing the parameter's values
     * @see #getParameter
     * @since 42.0.0
     */
    @Unstable
    default String[] getParameterValues(String name)
    {
        return null;
    }

    /**
     * Returns an <code>Enumeration</code> of <code>String</code> objects containing the names of the parameters
     * contained in this request. If the request has no parameters, the method returns an empty
     * <code>Enumeration</code>.
     *
     * @return an <code>Enumeration</code> of <code>String</code> objects, each <code>String</code> containing the name
     *         of a request parameter; or an empty <code>Enumeration</code> if the request has no parameters
     * @since 42.0.0
     */
    @Unstable
    default Enumeration<String> getParameterNames()
    {
        return Collections.emptyEnumeration();
    }

    /**
     * Returns the value of the named attribute as an <code>Object</code>, or <code>null</code> if no attribute of the
     * given name exists.
     *
     * @param name a <code>String</code> specifying the name of the attribute
     * @return an <code>Object</code> containing the value of the attribute, or <code>null</code> if the attribute does
     *         not exist
     * @since 42.0.0
     */
    @Unstable
    default Object getAttribute(String name)
    {
        return null;
    }

    /**
     * Stores an attribute in this request. Attributes are reset between requests.
     *
     * @param name a <code>String</code> specifying the name of the attribute
     * @param o the <code>Object</code> to be stored
     * @since 42.0.0
     */
    @Unstable
    default void setAttribute(String name, Object o)
    {

    }

    /**
     * Removes an attribute from this request. This method is not generally needed as attributes only persist as long as
     * the request is being handled.
     *
     * @param name a <code>String</code> specifying the name of the attribute to remove
     * @since 42.0.0
     */
    @Unstable
    default void removeAttribute(String name)
    {

    }

    /**
     * Returns an <code>Enumeration</code> containing the names of the attributes available to this request. This method
     * returns an empty <code>Enumeration</code> if the request has no attributes available to it.
     * 
     * @return an <code>Enumeration</code> of strings containing the names of the request's attributes
     * @since 42.0.0
     */
    @Unstable
    default Enumeration<String> getAttributeNames()
    {
        return Collections.emptyEnumeration();
    }

    // XWiki

    /**
     * @return the user that holds the responsibility, in terms of access rights, for the submitted data and the changes
     *         triggered by this request. If the request doesn't indicate an effective author then the user that gets
     *         authenticated with the information provided by this request (or the guest user, if authentication
     *         information is missing) should be considered the effective author.
     * @since 42.0.0
     */
    @Unstable
    default UserReference getEffectiveAuthor()
    {
        return null;
    }

    // Deprecated

    /**
     * @deprecated use {@link #getParameter(String)} or {@link #getAttribute(String)} instead depending on the need
     */
    @Deprecated(since = "42.0.0")
    default Object getProperty(String key)
    {
        Object result;

        // Look first in the Query Parameters and then in the Query Attributes
        result = getParameter(key);
        if (result == null) {
            result = getAttribute(key);
        }

        return result;
    }

    /**
     * @deprecated use {@link #getParameterValues(String)} or {@link #getAttribute(String)} instead depending on the
     *             need
     * @since 3.2M3
     */
    @Deprecated(since = "42.0.0")
    default List<Object> getProperties(String key)
    {
        List<Object> result = new ArrayList<>();

        // Look first in the parameters, and then in the attributes
        Object[] requestParameters = getParameterValues(key);
        if (requestParameters != null) {
            result.addAll(Arrays.asList(requestParameters));
        }
        Object attributeValue = getAttribute(key);
        if (attributeValue != null) {
            result.add(attributeValue);
        }

        return result;
    }

    /**
     * @deprecated use {@link #setAttribute(String, Object)} instead
     */
    @Deprecated(since = "42.0.0")
    default void setProperty(String key, Object value)
    {
        setAttribute(key, value);
    }

    /**
     * @deprecated use {@link #removeAttribute(String)} instead
     */
    @Deprecated(since = "42.0.0")
    default void removeProperty(String key)
    {
        removeAttribute(key);
    }
}
