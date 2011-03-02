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
package org.xwiki.velocity;

import java.util.Properties;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Configuration properties for the Velocity module.
 * <p>
 * You can override the default values for each of the configuration properties below by defining them in XWiki's
 * global configuration file using a prefix of "velocity" followed by the property name. For example:
 * <code>velocity.tools = listtool = org.apache.velocity.tools.generic.ListTool</code>
 *
 * @version $Id$
 * @since 2.0M1
 */
@ComponentRole
public interface VelocityConfiguration
{
    /**
     * @return the Velocity properties as defined at
     *         http://velocity.apache.org/engine/devel/developer-guide.html#Velocity_Configuration_Keys_and_Values
     */
    Properties getProperties();

    /**
     * @return the list of configured Velocity Tools (the key is the name under which the tool will be available
     *         in the Velocity Context and the value is the tool's class name) 
     */
    Properties getTools();
}
