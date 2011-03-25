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
package com.xpn.xwiki.plugin.test;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * Simple test plugin which replaces the whole content with a debug text ("It's working").
 * 
 * @version $Id$
 * @deprecated the plugin technology is deprecated
 */
@Deprecated
public class TestPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface
{
    /**
     * The mandatory plugin constructor, this is the method called (through reflection) by the plugin manager.
     * 
     * @param name the plugin name
     * @param className the name of this class, ignored
     * @param context the current request context
     */
    public TestPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String commonTagsHandler(String line, XWikiContext context)
    {
        return "It's working";
    }
}
