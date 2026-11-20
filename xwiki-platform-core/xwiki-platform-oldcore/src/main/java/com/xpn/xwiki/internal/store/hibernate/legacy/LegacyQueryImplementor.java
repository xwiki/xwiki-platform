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
import java.lang.reflect.Modifier;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.query.spi.QueryImplementor;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isEquals;
import static net.bytebuddy.matcher.ElementMatchers.isHashCode;
import static net.bytebuddy.matcher.ElementMatchers.isToString;
import static net.bytebuddy.matcher.ElementMatchers.not;

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

    private static final String HANDLER_FIELD = "handler";

    private final QueryImplementor<R> delegate;

    /**
     * The class of the wrapping queries, generated the first time it's needed.
     */
    private static final class ProxyClass
    {
        private static final Class<?> CLASS = new ByteBuddy()
            // The Hibernate types contain methods which only differ by their return type (the bridge methods using
            // the javax.persistence types), which are different methods for the JVM: each of them must be
            // implemented, and they can't be represented by a java.lang.reflect.Proxy
            .with(MethodGraph.Compiler.Default.forJVMHierarchy()).subclass(Object.class)
            .implement(QueryImplementor.class).defineField(HANDLER_FIELD, InvocationHandler.class, Modifier.PUBLIC)
            .method(not(isDeclaredBy(Object.class)).or(isEquals()).or(isHashCode()).or(isToString()))
            .intercept(InvocationHandlerAdapter.toField(HANDLER_FIELD)).make()
            .load(QueryImplementor.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();

        private ProxyClass()
        {
            // Holder
        }
    }

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
        try {
            Object proxy = ProxyClass.CLASS.getDeclaredConstructor().newInstance();
            ProxyClass.CLASS.getField(HANDLER_FIELD).set(proxy, new LegacyQueryImplementor<>(delegate));

            return (QueryImplementor<R>) proxy;
        } catch (ReflectiveOperationException e) {
            throw new HibernateException("Failed to wrap the query", e);
        }
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
