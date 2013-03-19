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
package org.xwiki.wikistream.internal.filter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Helper for input module taking care of calling the right event when it exist and simply ignores it when the listener
 * does not support it.
 * 
 * @version $Id$
 */
public class AllFilterInvocationHandler implements InvocationHandler
{
    private Object filter;

    private AllFilterInvocationHandler(Object filter)
    {
        this.filter = filter;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (method.getDeclaringClass().isAssignableFrom(this.filter.getClass())) {
            return method.invoke(this.filter, args);
        }

        return null;
    }

    public static AllFilter getProxy(Object listener)
    {
        return (AllFilter) Proxy.newProxyInstance(AllFilter.class.getClassLoader(),
            new Class[] {AllFilter.class}, new AllFilterInvocationHandler(listener));
    }
}
