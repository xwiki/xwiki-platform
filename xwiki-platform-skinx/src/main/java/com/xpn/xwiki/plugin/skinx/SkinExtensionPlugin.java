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

package com.xpn.xwiki.plugin.skinx;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * <p>
 * Skin Extensions base plugin. It allows templates and document content to pull required skin files in the generated
 * XHTML (or whatever XML) content.
 * </p>
 * <p>
 * The API provides a method {@link SkinExtensionPluginApi#use(String)}, which, when called, marks an extension as used
 * in the current result. Later on, all the used extensions are inserted in the content, by replacing the first
 * occurence of the following string: <tt>&lt;!-- canonical.plugin.classname --&gt;</tt>, where the actual extension
 * type classname is used. For example, JS extensions are inserted in place of
 * <tt>&lt;!-- com.xpn.xwiki.plugin.skinx.JsSkinExtensionPlugin --&gt;</tt>.
 * </p>
 * 
 * @see SkinExtensionPluginApi
 * @see JsSkinExtensionPlugin
 * @see CssSkinExtensionPlugin
 */
public abstract class SkinExtensionPlugin extends XWikiDefaultPlugin
{
    /** Log object to log messages in this class. */
    private static final Log LOG = LogFactory.getLog(SkinExtensionPlugin.class);

    /**
     * Abstract method for obtaining a link that points to the actual pulled resource. Each type of resource has its own
     * format for the link, for example Javascript uses <code>&lt;script src="/path/to/Document"&gt;</code>, while CSS
     * uses <code>&lt;link rel="stylesheet" href="/path/to/Document"&gt;</code> (the actual syntax is longer, this is
     * just a simplified example).
     * 
     * @param documentName the name of the wiki document holding the resource.
     * @param context the current request context, needed to access the URLFactory.
     * @return A <code>String</code> representation of the linking element that should be printed in the generated HTML.
     */
    public abstract String getLink(String documentName, XWikiContext context);

    /**
     * {@inheritDoc}
     * 
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public SkinExtensionPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getPluginApi
     */
    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new SkinExtensionPluginApi((SkinExtensionPlugin) plugin, context);
    }

    @SuppressWarnings("unchecked")
    protected Set<String> getRequestList(XWikiContext context)
    {
        initializeRequestListIfNeeded(context);
        return (Set<String>) context.get(this.getClass().getCanonicalName());
    }

    /**
     * Initializes the list of pulled extensions corresponding to this request, if it wasn't already initialized. This
     * method is not thread safe, since a context should not be shared among threads.
     * 
     * @param context The current context where this list is stored.
     */
    protected void initializeRequestListIfNeeded(XWikiContext context)
    {
        if (!context.containsKey(this.getClass().getCanonicalName())) {
            context.put(this.getClass().getCanonicalName(), new LinkedHashSet<String>());
        }
    }

    /**
     * Mark a skin extension document as used in the current result.
     * 
     * @param skinFile
     */
    public void use(String skinFile, XWikiContext context)
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Using [%s] as [%s] extension", skinFile, this.getName()));
        }
        getRequestList(context).add(skinFile);
    }

    /**
     * @return
     */
    public String getImportString(XWikiContext context)
    {
        StringBuilder iStr = new StringBuilder();
        for (String docName : getRequestList(context)) {
            iStr.append(getLink(docName, context));
        }
        return iStr.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#beginParsing(XWikiContext)
     */
    @Override
    public void beginParsing(XWikiContext context)
    {
        initializeRequestListIfNeeded(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#endParsing(String, XWikiContext)
     */
    @Override
    public String endParsing(String content, XWikiContext context)
    {
        // Using an XML comment is pretty safe, as extensions probably wouldn't work in other type
        // of documents, like RTF, CSV or JSON.
        String hook = "<!-- " + this.getClass().getCanonicalName() + " -->";
        String result = content.replaceFirst(hook, getImportString(context));
        return result;
    }
}
