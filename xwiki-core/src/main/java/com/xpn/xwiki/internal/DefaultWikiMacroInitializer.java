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
package com.xpn.xwiki.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.context.Execution;
import org.xwiki.rendering.macro.wikibridge.WikiMacroInitializer;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroFactory;
import org.xwiki.rendering.macro.wikibridge.WikiMacroException;
import org.xwiki.rendering.macro.wikibridge.WikiMacroManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * A {@link DefaultWikiMacroInitializer} providing wiki macros.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component
public class DefaultWikiMacroInitializer extends AbstractLogEnabled implements WikiMacroInitializer, WikiMacroConstants
{
    /**
     * Main wiki identifier.
     */
    private static final String MAIN_WIKI = "xwiki";

    /**
     * The {@link org.xwiki.rendering.macro.wikibridge.WikiMacroFactory} component.
     */
    @Requirement
    private WikiMacroFactory wikiMacroFactory;

    /**
     * The {@link WikiMacroManager} component.
     */
    @Requirement
    private WikiMacroManager wikiMacroManager;

    /**
     * The {@link Execution} component used for accessing XWikiContext.
     */
    @Requirement
    private Execution execution;

    /**
     * Utility method for accessing XWikiContext.
     * 
     * @return the XWikiContext.
     */
    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    /**
     * {@inheritDoc}
     */
    public void init()
    {
        // Install or Upgrade wiki macro class definitions.
        try {
            installOrUpgradeWikiMacroClasses();
        } catch (XWikiException ex) {
            getLogger().error("Error while installing / upgrading wiki macro class definitions.", ex);
            return;
        }

        XWikiContext xcontext = getContext();

        // Only consider the main wiki.
        xcontext.setDatabase(MAIN_WIKI);

        // Search for all those documents with macro definitions.
        // TODO: Use the query manager instead
        String sql =
            "select doc.fullName from XWikiDocument doc, BaseObject obj where doc.fullName=obj.name and obj.className=?";
        List<Object> wikiMacroDocs = null;
        try {
            wikiMacroDocs = xcontext.getWiki().getStore().search(sql, 0, 0, Arrays.asList(WIKI_MACRO_CLASS), xcontext);
        } catch (XWikiException ex) {
            getLogger().error("Error while searching for macro documents", ex);
            return;
        }

        // Build macros.
        Map<String, WikiMacro> wikiMacros = new HashMap<String, WikiMacro>();
        for (Object obj : wikiMacroDocs) {
            String wikiMacroDoc = (String) obj;
            try {
                WikiMacro macro = wikiMacroFactory.createWikiMacro(wikiMacroDoc);
                wikiMacros.put(wikiMacroDoc, macro);
            } catch (WikiMacroException ex) {
                // Just log the exception and skip to the next.
                getLogger().error(ex.getMessage(), ex);
            }
        }

        // Register the wiki macros against WikiMacroManager.
        for (String documentName : wikiMacros.keySet()) {
            // TODO: Fix this to allow macros to be registered for any wiki
            // TODO: In addition the main wiki name should never be hardcoded!!
            wikiMacroManager.registerWikiMacro(MAIN_WIKI + ":" + documentName, wikiMacros.get(documentName));
        }
    }
    
    private boolean setWikiMacroClassesDocumentFields(XWikiDocument doc, String title)
    {
        boolean needsUpdate = false;
        
        if (StringUtils.isBlank(doc.getCreator())) {
            needsUpdate = true;
            doc.setCreator(XWikiRightService.SUPERADMIN_USER);
        }
        if (StringUtils.isBlank(doc.getAuthor())) {
            needsUpdate = true;
            doc.setAuthor(doc.getCreator());
        }
        if (StringUtils.isBlank(doc.getParent())) {
            needsUpdate = true;
            doc.setParent("XWiki.XWikiClasses");
        }
        if (StringUtils.isBlank(doc.getTitle())) {
            needsUpdate = true;
            doc.setTitle(title);
        }
        if (StringUtils.isBlank(doc.getContent()) || !XWikiDocument.XWIKI20_SYNTAXID.equals(doc.getSyntaxId())) {
            needsUpdate = true;      
            doc.setContent("{{include document=\"XWiki.ClassSheet\" /}}");
            doc.setSyntaxId(XWikiDocument.XWIKI20_SYNTAXID);
        }
        
        return needsUpdate;
    }

    /**
     * Installs or upgrades XWiki.WikiMacroClass & XWiki.WikiMacroParameterClass.
     * 
     * @throws XWikiException if an error occurs while accessing wiki macro class definitions.
     */
    private void installOrUpgradeWikiMacroClasses() throws XWikiException
    {
        XWikiContext xcontext = getContext();

        // Install or Upgrade XWiki.WikiMacroClass
        XWikiDocument doc = xcontext.getWiki().getDocument(WIKI_MACRO_CLASS, xcontext);
        BaseClass bclass = doc.getxWikiClass();
        bclass.setName(WIKI_MACRO_CLASS);

        boolean needsUpdate = false;
        
        needsUpdate |= setWikiMacroClassesDocumentFields(doc, "XWiki Wiki Macro Class");
        needsUpdate |= bclass.addTextField(MACRO_ID_PROPERTY, "Macro id", 30);
        needsUpdate |= bclass.addTextField(MACRO_NAME_PROPERTY, "Macro name", 30);
        needsUpdate |= bclass.addTextAreaField(MACRO_DESCRIPTION_PROPERTY, "Macro description", 40, 5);
        needsUpdate |= bclass.addTextField(MACRO_DEFAULT_CATEGORY_PROPERTY, "Default category", 30);
        needsUpdate |= bclass.addBooleanField(MACRO_INLINE_PROPERTY, "Supports inline mode", "select");
        needsUpdate |= bclass.addStaticListField(MACRO_CONTENT_TYPE_PROPERTY, "Macro content type", 1, false,
            "Mandatory|Optional|No content", "select", "|");
        needsUpdate |= bclass.addTextAreaField(MACRO_CONTENT_DESCRIPTION_PROPERTY, 
            "Content description (Not applicable for \"No content\" type)", 40, 5);
        needsUpdate |= bclass.addTextAreaField(MACRO_CODE_PROPERTY, "Macro code", 40, 5);
        
        if (needsUpdate) {
           xcontext.getWiki().saveDocument(doc, xcontext); 
        }
        
        // Install or Upgrade XWiki.WikiMacroParameterClass
        doc = xcontext.getWiki().getDocument(WIKI_MACRO_PARAMETER_CLASS, xcontext);
        bclass = doc.getxWikiClass();
        bclass.setName(WIKI_MACRO_PARAMETER_CLASS);
        
        needsUpdate = false;
        
        needsUpdate |= setWikiMacroClassesDocumentFields(doc, "XWiki Wiki Macro Parameter Class");
        needsUpdate |= bclass.addTextField(PARAMETER_NAME_PROPERTY, "Parameter name", 30);
        needsUpdate |= bclass.addTextAreaField(PARAMETER_DESCRIPTION_PROPERTY, "Parameter description", 40, 5);
        needsUpdate |= bclass.addBooleanField(PARAMETER_MANDATORY_PROPERTY, "Parameter mandatory", "select");
        
        if (needsUpdate) {
            xcontext.getWiki().saveDocument(doc, xcontext); 
        }
    }
}
