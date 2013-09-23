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
package org.xwiki.rendering.internal.macro.script;

import org.xwiki.classloader.ExtendedURLClassLoader;
import org.xwiki.component.annotation.Role;

/**
 * Create a classloader that can load classes and resources from JARs specified as attachments of wiki pages.
 *  
 * @version $Id$
 * @since 2.0.1
 */
@Role
public interface AttachmentClassLoaderFactory
{
    /**
     * @param jarURLs the comma-separated lists of URLs pointing to the JARs that should be made visible in the 
     *        classloader.
     * @param parent the parent classloader in which to look first for a resource/class
     * @return the classloader in which the passed JARs are visible
     * @throws Exception in case of an error, for example if an attachment cannot be loaded
     */
    ExtendedURLClassLoader createAttachmentClassLoader(String jarURLs, ClassLoader parent) throws Exception;

    /**
     * Augment the passed classloader with the JARs definitions passed as parameter.
     * 
     * @param jarURLs the comma-separated lists of URLs pointing to the JARs that should be made visible in the 
     *        classloader.
     * @param source the classloader to augment
     * @throws Exception in case of an error, for example if an attachment cannot be loaded
     */
    void extendAttachmentClassLoader(String jarURLs, ExtendedURLClassLoader source) throws Exception;
}
