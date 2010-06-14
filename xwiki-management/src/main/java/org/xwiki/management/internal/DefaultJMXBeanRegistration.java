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
package org.xwiki.management.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.management.JMXBeanRegistration;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Registers MBeans against the default platform MBean Server using a default ObjectName domain of "org.xwiki".
 *
 * @version $Id$
 * @since 2.4M2
 */
@Component
public class DefaultJMXBeanRegistration extends AbstractLogEnabled implements JMXBeanRegistration
{
    /**
     * {@inheritDoc}
     * @see org.xwiki.management.JMXBeanRegistration#registerMBean(Object, String)
     */
    public void registerMBean(Object mbean, String name)
    {
        // Make sure we never fail since XWiki should execute correctly even if there's no MBean Server running.
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName oname = new ObjectName("org.xwiki:" + name);
            mbs.registerMBean(mbean, oname);
            getLogger().debug("Registered resource with name [" + name + "] successfully");
        } catch (Exception e) {
            // Failed to register the MBean, log a warning
            getLogger().warn("Failed to register resource with name [" + name + "]. Reason = [" + e.getMessage() + "]");
        }
    }
}
