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
 *
 */
package org.xwiki.velocity.introspection;

import java.lang.reflect.Method;

import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.Introspector;
import org.apache.velocity.util.introspection.Uberspect;
import org.apache.velocity.util.introspection.VelMethod;
import org.apache.velocity.util.introspection.VelPropertyGet;
import org.apache.velocity.util.introspection.VelPropertySet;

/**
 * Chainable Velocity Uberspector that checks for deprecated method calls. It does that by checking
 * if the returned method has a Deprecated annotation. Because this is a chainable uberspector, it
 * has to re-get the method using a default introspector, which is not safe; future uberspectors
 * might not be able to return a precise method name, or a method of the original target object.
 * 
 * @since 1.5M1
 * @see ChainableUberspector
 */
public class DeprecatedCheckUberspector extends ChainableUberspectorBase implements Uberspect,
    ChainableUberspector
{
    /**
     * {@inheritDoc Uberspect}
     * 
     * @see org.apache.velocity.util.introspection.UberspectImpl#init()
     */
    public void init()
    {
        super.init();
        this.introspector = new Introspector(log);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.velocity.introspection.ChainableUberspectorBase#getMethod(java.lang.Object,
     *      java.lang.String, java.lang.Object[], org.apache.velocity.util.introspection.Info)
     */
    @Override
    public VelMethod getMethod(Object obj, String methodName, Object[] args, Info i)
        throws Exception
    {
        VelMethod method = super.getMethod(obj, methodName, args, i);
        if (method != null) {
            Method m = introspector.getMethod(obj.getClass(), method.getMethodName(), args);
            if (m != null && m.isAnnotationPresent(Deprecated.class)) {
                log.warn(String.format("Deprecated usage of method [%s] in %s@%d,%d", obj
                    .getClass().getCanonicalName()
                    + "." + methodName, i.getTemplateName(), i.getLine(), i.getColumn()));
            }
        }
        return method;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.velocity.introspection.ChainableUberspectorBase#getPropertyGet(java.lang.Object,
     *      java.lang.String, org.apache.velocity.util.introspection.Info)
     */
    @Override
    public VelPropertyGet getPropertyGet(Object obj, String identifier, Info i) throws Exception
    {
        VelPropertyGet method = super.getPropertyGet(obj, identifier, i);
        if (method != null) {
            Method m =
                introspector.getMethod(obj.getClass(), method.getMethodName(), new Object[] {});
            if (m != null && m.isAnnotationPresent(Deprecated.class)) {
                log.warn(String.format("Deprecated usage of getter [%s] in %s@%d,%d", obj
                    .getClass().getCanonicalName()
                    + "." + method.getMethodName(), i.getTemplateName(), i.getLine(), i
                    .getColumn()));
            }
        }
        return method;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.velocity.introspection.ChainableUberspectorBase#getPropertySet(java.lang.Object,
     *      java.lang.String, java.lang.Object, org.apache.velocity.util.introspection.Info)
     */
    @Override
    public VelPropertySet getPropertySet(Object obj, String identifier, Object arg, Info i)
        throws Exception
    {
        // TODO Auto-generated method stub
        VelPropertySet method = super.getPropertySet(obj, identifier, arg, i);
        if (method != null) {
            Method m =
                introspector
                    .getMethod(obj.getClass(), method.getMethodName(), new Object[] {arg});
            if (m != null && m.isAnnotationPresent(Deprecated.class)) {
                log.warn(String.format("Deprecated usage of setter [%s] in %s@%d,%d", obj
                    .getClass().getCanonicalName()
                    + "." + method.getMethodName(), i.getTemplateName(), i.getLine(), i
                    .getColumn()));
            }
        }
        return method;
    }
}
