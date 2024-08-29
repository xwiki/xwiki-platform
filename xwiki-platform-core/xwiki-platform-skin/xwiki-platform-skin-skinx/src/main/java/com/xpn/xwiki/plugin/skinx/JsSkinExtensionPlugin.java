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

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.XWikiContext;

/**
 * Skin Extension plugin that allows pulling javascript code stored inside wiki documents as
 * <code>XWiki.JavaScriptExtension</code> objects.
 *
 * @version $Id$
 */
public class JsSkinExtensionPlugin extends AbstractDocumentSkinExtensionPlugin
{
    /**
     * The name of the XClass storing the code for this type of extensions.
     */
    public static final String JSX_CLASS_NAME = "XWiki.JavaScriptExtension";

    /**
     * The local reference of the XClass storing the code for this type of extensions.
     */
    public static final LocalDocumentReference JSX_CLASS_REFERENCE = new LocalDocumentReference("XWiki",
        "JavaScriptExtension");

    /**
     * The identifier for this plugin; used for accessing the plugin from velocity, and as the action returning the
     * extension content.
     */
    public static final String PLUGIN_NAME = "jsx";

    /**
     * XWiki plugin constructor.
     *
     * @param name The name of the plugin, which can be used for retrieving the plugin API from velocity. Unused.
     * @param className The canonical classname of the plugin. Unused.
     * @param context The current request context.
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#XWikiDefaultPlugin(String, String, com.xpn.xwiki.XWikiContext)
     */
    public JsSkinExtensionPlugin(String name, String className, XWikiContext context)
    {
        super(PLUGIN_NAME, className, context);
    }

    /**
     * {@inheritDoc}
     * <p>
     * We must override this method since the plugin manager only calls it for classes that provide their own
     * implementation, and not an inherited one.
     * </p>
     *
     * @see com.xpn.xwiki.plugin.XWikiPluginInterface#virtualInit(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public void virtualInit(XWikiContext context)
    {
        super.virtualInit(context);
    }

    @Override
    public String getLink(String documentName, XWikiContext context)
    {
        DocumentReference documentReference = getCurrentDocumentReferenceResolver().resolve(documentName);
        if (!isAccessible(documentReference, context)) {
            // No access to view the Skin Extension's document. Don`t generate any link to avoid a useless network
            // request always leading to a 403 Error.
            return "";
        }

        StringBuilder result = new StringBuilder("<script src='");
        result.append(XMLUtils.escapeAttributeValue(
            getDocumentSkinExtensionURL(documentReference, documentName, PLUGIN_NAME, context)));
        if (isDefer(documentName, context)) {
            result.append("' defer='defer");
        }
        result.append("'></script>\n");

        return result.toString();
    }

    @Override
    protected String getExtensionClassName()
    {
        return JSX_CLASS_NAME;
    }

    @Override
    protected String getExtensionName()
    {
        return "Javascript";
    }

    /**
     * {@inheritDoc}
     * <p>
     * We must override this method since the plugin manager only calls it for classes that provide their own
     * implementation, and not an inherited one.
     * </p>
     *
     * @see AbstractSkinExtensionPlugin#endParsing(String, XWikiContext)
     */
    @Override
    public String endParsing(String content, XWikiContext context)
    {
        return super.endParsing(content, context);
    }
}
