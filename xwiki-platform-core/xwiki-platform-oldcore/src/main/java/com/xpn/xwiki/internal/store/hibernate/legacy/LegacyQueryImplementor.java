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
package com.xpn.xwiki.internal.store.hibernate.legacy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import org.hibernate.query.spi.QueryImplementor;

/**
 * Wrap a QueryImplementor to convert the positional parameters from legacy 0-based Hibernate parameters to 1-based JPA
 * parameters.
 *
 * @param <R> query result type
 * @version $Id$
 * @since 11.5RC1
 * @deprecated
 */
@Deprecated
public final class LegacyQueryImplementor<R> implements InvocationHandler
{
    /**
     * The methods taking an int as first parameter which is not a parameter position.
     */
    private static final Set<String> NOT_POSITION_METHODS = Set.of("setFirstResult", "setMaxResults",
        "setFetchSize", "setTimeout", "setHibernateFirstResult", "setHibernateMaxResults");

    private final QueryImplementor<R> delegate;

    private LegacyQueryImplementor(QueryImplementor<R> delegate)
    {
        this.delegate = delegate;
    }

    /**
     * @param <R> query result type
     * @param delegate the actual query
     * @return the query converting the positional parameters
     * @since 18.10.0RC1
     */
    @SuppressWarnings("unchecked")
    public static <R> QueryImplementor<R> wrap(QueryImplementor<R> delegate)
    {
        return (QueryImplementor<R>) Proxy.newProxyInstance(QueryImplementor.class.getClassLoader(),
            new Class<?>[] {QueryImplementor.class}, new LegacyQueryImplementor<>(delegate));
    }

    private static boolean isPositionMethod(Method method)
    {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0 || parameterTypes[0] != int.class) {
            return false;
        }

        String name = method.getName();

        return (name.startsWith("set") || name.startsWith("getParameter")) && !NOT_POSITION_METHODS.contains(name);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        Object[] actualArgs = args;
        if (isPositionMethod(method)) {
            actualArgs = args.clone();
            actualArgs[0] = (Integer) args[0] + 1;
        }

        Object result;
        try {
            result = method.invoke(this.delegate, actualArgs);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }

        // Keep the fluent API going through the proxy
        return result == this.delegate ? proxy : result;
    }
}
