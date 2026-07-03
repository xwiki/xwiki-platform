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
package org.xwiki.tool.packager.internal;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.internal.AbstractExtensionHandler;
import org.xwiki.job.Request;

/**
 * JAR handler doing nothing. Just here so that JAR extension are not failing.
 * 
 * @version $Id$
 * @since 9.5RC1
 */
@Component(hints = { "jar", "webjar", "webjar-node" })
@Singleton
public class MavenBuildJarExtensionHandler extends AbstractExtensionHandler
{
    @Override
    public void install(LocalExtension localExtension, String namespace, Request request) throws InstallException
    {
        // Do nothing
    }

    @Override
    public void uninstall(InstalledExtension localExtension, String namespace, Request request)
        throws UninstallException
    {
        // Do nothing
    }
}
