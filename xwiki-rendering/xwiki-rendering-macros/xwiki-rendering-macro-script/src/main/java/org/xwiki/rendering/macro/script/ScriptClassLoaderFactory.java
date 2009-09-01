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

import org.xwiki.component.annotation.ComponentRole;

/**
 * Create Class Loader to be used when executing scripts, see {@link #createClassLoader(String)} for more details.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
@ComponentRole
public interface ScriptClassLoaderFactory
{
    /**
     * Create Class Loader to be used when executing scripts. The class loader is created based on some
     * passed reference specifying a list of jars to be made available in the created class loader. The format
     * is not defined and left to implementation classes.
     * 
     * @param scriptJars the list of jars to make available in the returned class loader
     * @return the configured class loader
     * @throws Exception if the class loader fails to be created for any reason (eg some invalid URL passed)
     */
    ClassLoader createClassLoader(String scriptJars) throws Exception;
}
