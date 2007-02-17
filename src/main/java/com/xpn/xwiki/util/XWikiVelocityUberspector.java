/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.util;

import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.util.RuntimeServicesAware;
import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.SecureIntrospectorControl;
import org.apache.velocity.util.introspection.SecureIntrospectorImpl;
import org.apache.velocity.util.introspection.UberspectImpl;

import java.util.Iterator;

/**
 * Temporary Velocity Uberspector that prevents classloader related method calls. Use this
 * introspector for situations in which template writers are numerous or untrusted. Specifically,
 * this introspector prevents creation of arbitrary objects or reflection on objects. It is
 * temporary because we've had to create this class as the SecureUberspector one implemented in
 * Velocity has an important bug (see https://issues.apache.org/jira/browse/VELOCITY-516). Once
 * that bug is fixed removed this class and update the velocity.properties file.
 *
 * @version $Id: $
 */
public class XWikiVelocityUberspector extends UberspectImpl implements RuntimeServicesAware
{
    RuntimeServices runtimeServices;

    public XWikiVelocityUberspector()
    {
        super();
    }

    /**
     * init - generates the Introspector. As the setup code makes sure that the log gets set
     * before this is called, we can initialize the Introspector using the log object.
     */
    public void init()
    {
        String[] badPackages = runtimeServices.getConfiguration()
            .getStringArray(RuntimeConstants.INTROSPECTOR_RESTRICT_PACKAGES);

        String[] badClasses = runtimeServices.getConfiguration()
            .getStringArray(RuntimeConstants.INTROSPECTOR_RESTRICT_CLASSES);

        introspector = new SecureIntrospectorImpl(badClasses, badPackages, log);
    }

    /**
     * Get an iterator from the given object.  Since the superclass method this secure version
     * checks for execute permission.
     *
     * @param obj object to iterate over
     * @param i line, column, template info
     * @return Iterator for object
     */
    public Iterator getIterator(Object obj, Info i)
        throws Exception
    {
        if ((obj != null) &&
            !((SecureIntrospectorControl) introspector)
                .checkObjectExecutePermission(obj.getClass(), "iterator"))
        {
            log.warn("Cannot retrieve iterator from object of class " +
                obj.getClass().getName() +
                " due to security restrictions.");
            return null;
        } else {
            return super.getIterator(obj, i);
        }
    }

    /**
     * Store the RuntimeServices before the object is initialized..
     *
     * @param rs RuntimeServices object for initialization
     */
    public void setRuntimeServices(RuntimeServices rs)
    {
        this.runtimeServices = rs;
    }
}
