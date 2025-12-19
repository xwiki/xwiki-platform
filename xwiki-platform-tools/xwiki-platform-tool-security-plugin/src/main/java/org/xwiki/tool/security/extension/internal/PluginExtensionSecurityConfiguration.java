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
package org.xwiki.tool.security.extension.internal;

import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.security.ExtensionSecurityConfiguration;

/**
 * Replace the default {@link ExtensionSecurityConfiguration}.
 * 
 * @version $Id$
 * @since 18.0.0RC1
 */
@Component
@Singleton
public class PluginExtensionSecurityConfiguration implements ExtensionSecurityConfiguration
{
    @Override
    public boolean isSecurityScanEnabled()
    {
        return false;
    }

    @Override
    public int getScanDelay()
    {
        return 0;
    }

    @Override
    public String getScanURL()
    {
        return "https://api.osv.dev/v1/query";
    }

    @Override
    public String getReviewsURL()
    {
        return "https://extensions.xwiki.org/xwiki/bin/view/Extension/Extension/Security/Code/Reviews";
    }

}
