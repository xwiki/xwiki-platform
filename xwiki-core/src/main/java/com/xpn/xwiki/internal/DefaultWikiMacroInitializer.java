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
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroException;
import org.xwiki.rendering.macro.wikibridge.WikiMacroFactory;
import org.xwiki.rendering.macro.wikibridge.WikiMacroInitializer;
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
    public void registerExistingWikiMacros() throws Exception
    {                
        XWikiContext xcontext = getContext();
        
        // Check whether xwiki classes required for defining wiki macros are present.
        XWikiDocument wikiMacroClass = xcontext.getWiki().getDocument(WIKI_MACRO_CLASS, xcontext);
        XWikiDocument wikiMacroParameterClass = xcontext.getWiki().getDocument(WIKI_MACRO_PARAMETER_CLASS, xcontext);
        if (wikiMacroClass.isNew() || wikiMacroParameterClass.isNew()) {
            String message = "Unable to locate [%s] & [%s] classes required for defining wiki macros.";
            throw new Exception(String.format(message, WIKI_MACRO_CLASS, WIKI_MACRO_PARAMETER_CLASS));
        }

        // Register the wiki macros that exist in each wiki
        String originalWiki = xcontext.getDatabase();
        try {
            // If we're in multi wiki mode get the list of all subwikis, otherwise just look into the main wiki
            List<String> wikiNames;
            if (xcontext.getWiki().isVirtualMode()) {
                wikiNames = xcontext.getWiki().getVirtualWikisDatabaseNames(xcontext);
            } else {
                wikiNames = Collections.singletonList(xcontext.getMainXWiki());
            }

            for (String wikiName : wikiNames) {
                // Set the context to be in that wiki so that both the search for XWikiMacro class objects and the
                // registration of macros refistered for the current wiki will work.
                // TODO: In the future when we have APIs for it, move the code to set the current wiki and the current
                // user (see below) to the WikiMacroManager's implementation.
                xcontext.setDatabase(wikiName);

                // Search for all those documents with macro definitions and for each register the macro
                for (Object[] wikiMacroDocumentData : getWikiMacroDocumentData(xcontext)) {
                    // In the database the space and page names are always specified for a document. However the wiki
                    // part isn't so we need to replace the wiki reference with the current wiki.
                    DocumentReference wikiMacroDocumentReference = new DocumentReference(
                        (String) wikiMacroDocumentData[1], new SpaceReference((String) wikiMacroDocumentData[0],
                            new WikiReference(wikiName)));

                    String wikiMacroDocumentAuthor = (String) wikiMacroDocumentData[2];
                    try {
                        WikiMacro macro = wikiMacroFactory.createWikiMacro(wikiMacroDocumentReference);

                        // Set the author in the context to be the author who last modified the document containing
                        // the wiki macro class definition, so that if the Macro has the "Current User" visibility
                        // the correct user will be found in the Execution Context.
                        String originalAuthor = xcontext.getUser();
                        try {
                            xcontext.setUser(wikiMacroDocumentAuthor);
                            wikiMacroManager.registerWikiMacro(wikiMacroDocumentReference, macro);
                        } finally {
                            xcontext.setUser(originalAuthor);
                        }
                    } catch (WikiMacroException ex) {
                        // Just log the exception and skip to the next.
                        getLogger().error(ex.getMessage(), ex);
                    }
                }
            }
        } finally {
            xcontext.setDatabase(originalWiki);
        }
    }

    private List<Object[]> getWikiMacroDocumentData(XWikiContext xcontext) throws Exception
    {
        // TODO: Use the query manager instead
        String sql = "select doc.space, doc.name, doc.author from XWikiDocument doc, BaseObject obj where "
            + "doc.fullName=obj.name and obj.className=?";
        List<Object[]> wikiMacroDocumentData;
        try {
            wikiMacroDocumentData = xcontext.getWiki().getStore().search(sql, 0, 0, Arrays.asList(WIKI_MACRO_CLASS),
                xcontext);
        } catch (XWikiException ex) {
            throw new Exception("Error while searching for macro documents", ex);
        }

        return wikiMacroDocumentData;
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
     * {@inheritDoc}
     */
    public void installOrUpgradeWikiMacroClasses() throws Exception
    {
        XWikiContext xcontext = getContext();

        // Install or Upgrade XWiki.WikiMacroClass
        XWikiDocument doc = xcontext.getWiki().getDocument(WIKI_MACRO_CLASS, xcontext);
        BaseClass bclass = doc.getXClass();
        bclass.setName(WIKI_MACRO_CLASS);

        boolean needsUpdate = false;
        
        needsUpdate |= setWikiMacroClassesDocumentFields(doc, "XWiki Wiki Macro Class");
        needsUpdate |= bclass.addTextField(MACRO_ID_PROPERTY, "Macro id", 30);
        needsUpdate |= bclass.addTextField(MACRO_NAME_PROPERTY, "Macro name", 30);
        needsUpdate |= bclass.addTextAreaField(MACRO_DESCRIPTION_PROPERTY, "Macro description", 40, 5);
        needsUpdate |= bclass.addTextField(MACRO_DEFAULT_CATEGORY_PROPERTY, "Default category", 30);
        needsUpdate |= bclass.addBooleanField(MACRO_INLINE_PROPERTY, "Supports inline mode", "yesno");
        needsUpdate |= bclass.addStaticListField(MACRO_VISIBILITY_PROPERTY, "Macro visibility", 1, false,
            "Current User|Current Wiki|Global", "select", "|");
        needsUpdate |= bclass.addStaticListField(MACRO_CONTENT_TYPE_PROPERTY, "Macro content type", 1, false,
            "Mandatory|Optional|No content", "select", "|");
        needsUpdate |= bclass.addTextAreaField(MACRO_CONTENT_DESCRIPTION_PROPERTY,
            "Content description (Not applicable for \"No content\" type)", 40, 5);
        needsUpdate |= bclass.addTextAreaField(MACRO_CODE_PROPERTY, "Macro code", 40, 20);

        if (needsUpdate) {
            update(doc);
        }

        // Install or Upgrade XWiki.WikiMacroParameterClass
        doc = xcontext.getWiki().getDocument(WIKI_MACRO_PARAMETER_CLASS, xcontext);
        bclass = doc.getXClass();
        bclass.setName(WIKI_MACRO_PARAMETER_CLASS);

        needsUpdate = false;

        needsUpdate |= setWikiMacroClassesDocumentFields(doc, "XWiki Wiki Macro Parameter Class");
        needsUpdate |= bclass.addTextField(PARAMETER_NAME_PROPERTY, "Parameter name", 30);
        needsUpdate |= bclass.addTextAreaField(PARAMETER_DESCRIPTION_PROPERTY, "Parameter description", 40, 5);
        needsUpdate |= bclass.addBooleanField(PARAMETER_MANDATORY_PROPERTY, "Parameter mandatory", "yesno");

        if (needsUpdate) {
            update(doc);
        }
    }

    /**
     * Utility method for updating a wiki macro class definition document.
     * 
     * @param doc xwiki document containing the wiki macro class.
     * @throws XWikiException if an error occurs while saving the document.
     */
    private void update(XWikiDocument doc) throws Exception
    {
        XWikiContext xcontext = getContext();

        if (doc.isNew()) {
            doc.setParent("XWiki.WebHome");
        }
        
        xcontext.getWiki().saveDocument(doc, xcontext);
    }
}
