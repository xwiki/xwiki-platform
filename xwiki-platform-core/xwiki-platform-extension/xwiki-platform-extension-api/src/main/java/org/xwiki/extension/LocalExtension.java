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

/**
 * Represent a local extension.
 * <p>
 * A local extension is an extension generally downloaded from a remote repossitory and now stored in the local
 * repository.
 * 
 * @version $Id$
 */
public interface LocalExtension extends Extension
{
    /**
     * Custom property key containing {@link #getFile()}.
     */
    String PKEY_FILE = "local.file";

    /**
     * Custom property key containing {@link #isInstalled()}.
     */
    String PKEY_INSTALLED = "local.installed";

    /**
     * Custom property key containing {@link #isDependency()}.
     */
    String PKEY_DEPENDENCY = "local.dependency";

    /**
     * @return the extension file in the filesystem
     */
    File getFile();

    /**
     * @return indicate if the extension is installed
     */
    boolean isInstalled();

    /**
     * Indicate if the extension is installed in the provided namespace.
     * 
     * @param namespace the namespace to look at, if null it means the extension is installed for all the namespaces
     * @return true if the extension is installed in the provided namespace
     */
    boolean isInstalled(String namespace);

    /**
     * @return the namespaces in which this extension is enabled. Null means root namespace (i.e all namespaces).
     */
    Collection<String> getNamespaces();

    /**
     * Indicate if the extension as been installed as a dependency of another one.
     * <p>
     * The idea is to be able to make the difference between extension specifically installed by a user so that it's
     * possible to know which extension are not really required anymore.
     * 
     * @return true if the the extension has been installed only because it was a dependency of another extension
     */
    boolean isDependency();
}
