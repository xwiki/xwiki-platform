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
package org.xwiki.rendering.macro.script;

import java.net.URL;
import java.util.List;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Create JAR URLs to be used in the script Class Loader when executing scripts, see {@link #createJARURLs(String)}
 * for more details.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
@ComponentRole
public interface ScriptJARURLFactory
{
    /**
     * Create a list of JAR URLs to be put in the Class Loader used when executing scripts. The URLs are created based
     * on some passed reference specifying a list of JARs to be made available in the created class loader. The format
     * is not defined and left to implementation classes.
     * 
     * @param scriptJars the list of JARs to make available in the returned class loader
     * @return the JAR URLs
     * @throws Exception if the URLs fail to be created for any reason (eg some invalid URL reference passed)
     */
    List<URL> createJARURLs(String scriptJars) throws Exception;
}
