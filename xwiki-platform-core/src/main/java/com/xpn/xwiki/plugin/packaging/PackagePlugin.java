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
 *
 */

package com.xpn.xwiki.plugin.packaging;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

public class PackagePlugin extends XWikiDefaultPlugin implements XWikiPluginInterface
{

    public PackagePlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    public String getName()
    {
        return "package";
    }

    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        try {
            return new PackageAPI(new Package(), context);
        } catch (PackageException e) {
            return null;
        }
    }

    public void flushCache()
    {
    }

    public void init(XWikiContext context)
    {
        super.init(context);
    }
}
