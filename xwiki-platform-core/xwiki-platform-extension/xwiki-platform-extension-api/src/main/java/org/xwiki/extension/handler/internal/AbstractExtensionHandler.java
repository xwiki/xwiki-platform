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
package org.xwiki.extension.handler.internal;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.ExtensionHandler;

/**
 * Base class for {@link ExtensionHandler} implementations.
 * 
 * @version $Id$
 */
public abstract class AbstractExtensionHandler implements ExtensionHandler
{
    /**
     * The logger to log.
     */
    @Inject
    protected Logger logger;

    @Override
    public void upgrade(LocalExtension previousLocalExtension, LocalExtension newLocalExtension, String namespace,
        Map<String, ? > extra) throws InstallException
    {
        try {
            uninstall(previousLocalExtension, namespace, extra);
        } catch (UninstallException e) {
            throw new InstallException("Failed to uninstall previous extension [" + previousLocalExtension + "]");
        }
        install(newLocalExtension, namespace, null);
    }

    @Override
    public void initialize(LocalExtension localExtension, String namespace) throws ExtensionException
    {
        // do nothing by default
    }
}
