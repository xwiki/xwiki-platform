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
package org.xwiki.rendering.configuration;

/**
 * Configuration properties for the Rendering module.
 * <p>
 * You can override the default values for each of the configuration properties below by defining them in XWiki's
 * global configuration file using a prefix of "rendering" followed by the property name. For example:
 * <code>rendering.linkLabelFormat = %s.%p</code>
 *
 * @version $Id: $
 * @since 1.6M1
 */
public interface RenderingConfiguration
{
    /**
     * This component's role, used when code needs to look it up.
     */
    String ROLE = RenderingConfiguration.class.getName();
    
    /**
     * A link label format is the format used to decide how to display links that have no label.
     * By default the page name is displayed. However it's possible to customize it using the
     * following tokens:
     * <ul>
     *   <li><strong>%w</strong>: wiki name</li>
     *   <li><strong>%s</strong>: space name</li>
     *   <li><strong>%p</strong>: page name</li>
     *   <li><strong>%P</strong>: camel cased page name, i.e. "My Page")</li>
     *   <li><strong>%t</strong>: page title</li>
     * </ul>
     * The default is "%p". Some examples: "%s.%p", "%w:%s.%p".
     *
     * @return the format to use to display link labels when the user hasn't specified a label
     */
    String getLinkLabelFormat();
}
