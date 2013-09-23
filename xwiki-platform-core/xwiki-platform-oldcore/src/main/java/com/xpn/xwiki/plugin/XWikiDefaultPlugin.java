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
package com.xpn.xwiki.plugin;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Abstract base plugin implementation.
 * 
 * @version $Id$
 * @deprecated the plugin technology is deprecated, consider rewriting as components
 */
@Deprecated
public class XWikiDefaultPlugin implements XWikiPluginInterface
{
    /**
     * The plugin name.
     * 
     * @see #getName()
     */
    private String name;

    /**
     * The mandatory plugin constructor, this is the method called (through reflection) by the plugin manager.
     * 
     * @param name the plugin name, usually ignored, since plugins have a fixed name
     * @param className the name of this class, ignored
     * @param context the current request context
     */
    public XWikiDefaultPlugin(String name, String className, XWikiContext context)
    {
        setClassName(className);
        setName(name);
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        // No public API by default
        return null;
    }

    @Override
    public void init(XWikiContext context)
    {
        // The default is to do nothing
    }

    @Override
    public void virtualInit(XWikiContext context)
    {
        // The default is to do nothing
    }

    @Override
    public void flushCache(XWikiContext context)
    {
        flushCache();
    }

    /**
     * Older equivalent of the {@link #flushCache(XWikiContext)} method without a context provided.
     * 
     * @deprecated use {@link #flushCache(XWikiContext)} instead
     */
    @Deprecated
    public void flushCache()
    {
        // The default is to do nothing
    }

    @Override
    public void beginParsing(XWikiContext context)
    {
        // The default is to do nothing
    }

    @Override
    public void beginRendering(XWikiContext context)
    {
        // The default is to do nothing
    }

    @Override
    public String commonTagsHandler(String content, XWikiContext context)
    {
        // The default is to do nothing, just return back the same content
        return content;
    }

    @Override
    public String startRenderingHandler(String content, XWikiContext context)
    {
        // The default is to do nothing, just return back the same content
        return content;
    }

    @Override
    public String outsidePREHandler(String line, XWikiContext context)
    {
        // The default is to do nothing, just return back the same content
        return line;
    }

    @Override
    public String insidePREHandler(String line, XWikiContext context)
    {
        // The default is to do nothing, just return back the same content
        return line;
    }

    @Override
    public String endRenderingHandler(String content, XWikiContext context)
    {
        // The default is to do nothing, just return back the same content
        return content;
    }

    @Override
    public void endRendering(XWikiContext context)
    {
        // The default is to do nothing
    }

    @Override
    public String endParsing(String content, XWikiContext context)
    {
        return content;
    }

    @Override
    public XWikiAttachment downloadAttachment(XWikiAttachment attachment, XWikiContext context)
    {
        // The default is to do nothing, just return the original attachment
        return attachment;
    }

    /**
     * Set the plugin name. Don't use outside the constructor.
     * 
     * @param name the new name of the plugin
     * @deprecated most plugins hard code their names, so this doesn't really work
     */
    @Deprecated
    public void setName(String name)
    {
        // Shouldn't really change the name of the plugin, but for backwards compatibility...
        this.name = name;
    }

    /**
     * Old method that doesn't really work. Don't use.
     * 
     * @return the name of the plugin
     * @deprecated use {@link #getName()} instead
     */
    @Deprecated
    public String getClassName()
    {
        return this.name;
    }

    /**
     * Old method that doesn't really work. Don't use.
     * 
     * @param name the new name of the plugin
     * @deprecated most plugins hard code their names, so this doesn't really work, and changing the classname isn't
     *             really possible
     */
    @Deprecated
    public void setClassName(String name)
    {
        // Shouldn't really change the name of the plugin, but for backwards compatibility...
        this.name = name;
    }
}
