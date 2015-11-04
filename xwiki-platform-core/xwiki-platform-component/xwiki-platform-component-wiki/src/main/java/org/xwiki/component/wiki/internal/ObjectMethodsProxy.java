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
package org.xwiki.component.wiki.internal;

import java.lang.reflect.Method;

/**
 * Handles calls to method from the {@link Object} class on a Proxy object that doesn't implement them.
 *
 * @version $Id$
 * @since 4.3M2
 */
public final class ObjectMethodsProxy
{
    /**
     * Default constructor.
     */
    private ObjectMethodsProxy()
    {
        // Nothing to do here.
    }

    /**
     * Proxies a method of the {@link Object} class.
     *
     * @param proxy the proxy instance
     * @param method the method to proxy
     * @param args possible arguments to the method invocation
     * @return the result of the proxied call
     */
    public static Object invoke(Object proxy, Method method, Object[] args)
    {
        try {
            if (method.equals(Object.class.getMethod("hashCode"))) {
                return proxyHashCode(proxy);
            } else if (method.equals(Object.class.getMethod("equals", new Class[] {Object.class}))) {
                return proxyEquals(proxy, args[0]);
            } else if (method.equals(Object.class.getMethod("toString"))) {
                return proxyToString(proxy);
            } else {
                throw new InternalError("unexpected Object method dispatched: " + method);
            }
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    /**
     * Default behavior for {@link Object#hashCode()} when not overridden in the wiki component definition.
     *
     * @param proxy the proxy object
     * @return a hash code for the proxy object, as if using standard {Object{@link #hashCode()}.
     */
    private static Integer proxyHashCode(Object proxy)
    {
        return System.identityHashCode(proxy);
    }

    /**
     * Default behavior for {@link Object#equals(Object)} when not overridden in the wiki component definition.
     *
     * @param proxy the proxy object
     * @param other the other object of the comparison
     * @return the result of the equality comparison between the passed proxy and other object
     */
    private static Boolean proxyEquals(Object proxy, Object other)
    {
        return (proxy == other ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Default behavior for {@link Object#toString()} when not overridden in the wiki component definition.
     *
     * @param proxy the proxy object
     * @return the String representation of the passed proxy object
     */
    private static String proxyToString(Object proxy)
    {
        return proxy.getClass().getName() + '@' + Integer.toHexString(proxy.hashCode());
    }


}
