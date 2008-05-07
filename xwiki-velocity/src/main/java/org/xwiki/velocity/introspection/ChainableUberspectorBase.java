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

import java.util.Iterator;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.Log;
import org.apache.velocity.util.RuntimeServicesAware;
import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.Uberspect;
import org.apache.velocity.util.introspection.UberspectImpl;
import org.apache.velocity.util.introspection.UberspectLoggable;
import org.apache.velocity.util.introspection.VelMethod;
import org.apache.velocity.util.introspection.VelPropertyGet;
import org.apache.velocity.util.introspection.VelPropertySet;

/**
 * Default implementation of a {@link ChainableUberspector chainable uberspector} that forwards all
 * calls to the wrapped uberspector (when that is possible, as it may not implement
 * RuntimeServicesAware, for example). It should be used as the base for all chainable uberspectors.
 * 
 * @since 1.5M1
 * @see ChainableUberspector
 */
public class ChainableUberspectorBase extends UberspectImpl implements ChainableUberspector,
    RuntimeServicesAware, UberspectLoggable
{
    /** The wrapped (decorated) uberspector. */
    protected Uberspect inner;

    /**
     * {@inheritDoc}
     * <p>
     * This implementation forwards the call to the wrapped uberspector, catching all exceptions.
     * </p>
     * 
     * @see org.apache.velocity.util.introspection.UberspectImpl#init()
     */
    @Override
    public void init()
    {
        try {
            inner.init();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation stores the wrapped uberspector in the protected {@link #inner} member.
     * </p>
     * 
     * @see ChainableUberspector#wrap(org.apache.velocity.util.introspection.Uberspect)
     * @see #inner
     */
    public void wrap(Uberspect inner)
    {
        this.inner = inner;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation forwards the call to the wrapped uberspector.
     * </p>
     * 
     * @see org.apache.velocity.util.introspection.UberspectImpl#getIterator(java.lang.Object,
     *      org.apache.velocity.util.introspection.Info)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Iterator getIterator(Object obj, Info i) throws Exception
    {
        return (inner != null) ? inner.getIterator(obj, i) : null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation forwards the call to the wrapped uberspector.
     * </p>
     * 
     * @see org.apache.velocity.util.introspection.UberspectImpl#getMethod(java.lang.Object,
     *      java.lang.String, java.lang.Object[], org.apache.velocity.util.introspection.Info)
     */
    @Override
    public VelMethod getMethod(Object obj, String methodName, Object[] args, Info i)
        throws Exception
    {
        return (inner != null) ? inner.getMethod(obj, methodName, args, i) : null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation forwards the call to the wrapped uberspector.
     * </p>
     * 
     * @see org.apache.velocity.util.introspection.UberspectImpl#getPropertyGet(java.lang.Object,
     *      java.lang.String, org.apache.velocity.util.introspection.Info)
     */
    @Override
    public VelPropertyGet getPropertyGet(Object obj, String identifier, Info i) throws Exception
    {
        return (inner != null) ? inner.getPropertyGet(obj, identifier, i) : null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation forwards the call to the wrapped uberspector.
     * </p>
     * 
     * @see org.apache.velocity.util.introspection.UberspectImpl#getPropertySet(java.lang.Object,
     *      java.lang.String, java.lang.Object, org.apache.velocity.util.introspection.Info)
     */
    @Override
    public VelPropertySet getPropertySet(Object obj, String identifier, Object arg, Info i)
        throws Exception
    {
        return (inner != null) ? inner.getPropertySet(obj, identifier, arg, i) : null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation stores the log in the current object and forwards the call to the wrapped
     * uberspector, if it also implements <code>UberspectLoggable</code>.
     * </p>
     * 
     * @see org.apache.velocity.util.introspection.UberspectLoggable#setLog(org.apache.velocity.runtime.log.Log)
     */
    @Override
    public void setLog(Log log)
    {
        this.log = log;
        if (inner instanceof UberspectLoggable) {
            ((UberspectLoggable) inner).setLog(log);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation forwards the call to the wrapped uberspector, if it also implements
     * <code>RuntimeServicesAware</code>.
     * </p>
     * 
     * @see org.apache.velocity.util.RuntimeServicesAware#setRuntimeServices(org.apache.velocity.runtime.RuntimeServices)
     */
    public void setRuntimeServices(RuntimeServices rs)
    {
        if (inner instanceof RuntimeServicesAware) {
            ((RuntimeServicesAware) inner).setRuntimeServices(rs);
        }
    }
}
