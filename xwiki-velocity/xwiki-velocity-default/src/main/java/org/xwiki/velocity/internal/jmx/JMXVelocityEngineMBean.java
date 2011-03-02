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
package org.xwiki.velocity.internal.jmx;

import javax.management.openmbean.TabularData;

/**
 * MBean API related to Velocity Engines. Supports the following features:
 * <ul>
 *   <li>Retrieve list of template namespaces along with the name of macros registered in each template namespace</li>
 * </ul>
 *
 * @version $Id$
 * @since 2.4M2
 */
public interface JMXVelocityEngineMBean
{
    /**
     * @return the list of template namespaces along with the name of macros registered in each template namespace
     */
    TabularData getTemplates();
}
