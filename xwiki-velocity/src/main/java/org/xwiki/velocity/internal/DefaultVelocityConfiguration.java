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
package org.xwiki.velocity.internal;

import java.util.Properties;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.velocity.VelocityConfiguration;

/**
 * All configuration options for the Velocity subsystem.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
public class DefaultVelocityConfiguration implements Initializable, VelocityConfiguration
{
    /**
     * Prefix for configuration keys for the Velocity module.
     */
    private static final String PREFIX = "velocity.";

    /**
     * Defines from where to read the rendering configuration data. 
     */
    @Requirement
    private ConfigurationSource configuration;

    /**
     * Default Tools.
     */
    private Properties defaultTools = new Properties(); 
    
    /**
     * Default properties.
     */
    private Properties defaultProperties = new Properties();
    
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // Default Velocity tools.
        this.defaultTools.setProperty("listtool", "org.apache.velocity.tools.generic.ListTool");
        this.defaultTools.setProperty("numbertool", "org.apache.velocity.tools.generic.NumberTool");
        this.defaultTools.setProperty("datetool", "org.apache.velocity.tools.generic.DateTool");
        this.defaultTools.setProperty("mathtool", "org.apache.velocity.tools.generic.MathTool");
        this.defaultTools.setProperty("sorttool", "org.apache.velocity.tools.generic.SortTool");
        this.defaultTools.setProperty("escapetool", "org.apache.velocity.tools.generic.EscapeTool");

        // Default Velocity properties
        this.defaultProperties.setProperty("resource.loader", "webapp");
        this.defaultProperties.setProperty("webapp.resource.loader.class", 
            "org.apache.velocity.tools.view.servlet.WebappLoader");
        this.defaultProperties.setProperty("velocimacro.messages.on", Boolean.FALSE.toString());
        this.defaultProperties.setProperty("resource.manager.logwhenfound", Boolean.FALSE.toString());
        this.defaultProperties.setProperty("velocimacro.permissions.allow.inline.local.scope", "true");
        // Prevents users from writing dangerous Velocity code like using Class.forName or Java threading APIs.
        this.defaultProperties.setProperty("runtime.introspector.uberspect", 
            "org.xwiki.velocity.introspection.ChainingUberspector");
        this.defaultProperties.setProperty("runtime.introspector.uberspect.chainClasses", 
            "org.apache.velocity.util.introspection.SecureUberspector,"
            + "org.xwiki.velocity.introspection.DeprecatedCheckUberspector");
    }

    /**
     * {@inheritDoc}
     * 
     * @see VelocityConfiguration#getProperties()
     */
    public Properties getProperties()
    {
        // Merge default properties and properties defined in the configuration
        Properties props = new Properties();
        props.putAll(this.defaultProperties);
        props.putAll(this.configuration.getProperty(PREFIX + "properties", Properties.class));
        return props;
    }

    /**
     * {@inheritDoc}
     * 
     * @see VelocityConfiguration#getTools()
     */
    public Properties getTools()
    {
        // Merge default tools and tools defined in the configuration
        Properties props = new Properties();
        props.putAll(this.defaultTools);
        props.putAll(this.configuration.getProperty(PREFIX + "tools", Properties.class));
        return props;
    }
}
