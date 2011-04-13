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
package org.xwiki.extension;

import java.io.File;
import java.util.Collection;

public interface LocalExtension extends Extension
{
    String PKEY_FILE = "local.file";

    String PKEY_INSTALLED = "local.installed";

    String PKEY_DEPENDENCY = "local.dependency";

    File getFile();

    boolean isInstalled();

    boolean isInstalled(String namespace);

    /**
     * @return the namespaces in which this extension is enabled. Null means root namespace (i.e all namespaces).
     */
    Collection<String> getNamespaces();

    /**
     * @return true if the the extension has been installed only because it was a dependency of another extension
     *         installer by user
     */
    boolean isDependency();
}
