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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.context.Execution;
import org.xwiki.rendering.macro.wikibridge.WikiMacroInitializer;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroBuilder;
import org.xwiki.rendering.macro.wikibridge.WikiMacroBuilderException;
import org.xwiki.rendering.macro.wikibridge.WikiMacroManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * A {@link DefaultWikiMacroInitializer} providing wiki macros.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component
public class DefaultWikiMacroInitializer extends AbstractLogEnabled implements WikiMacroInitializer
{
    /**
     * Main wiki identifier.
     */
    private static final String MAIN_WIKI = "xwiki";

    /**
     * The {@link WikiMacroBuilder} component.
     */
    @Requirement
    private WikiMacroBuilder wikiMacroBuilder;

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
        XWikiContext xcontext = getContext();

        // Only consider the main wiki.
        xcontext.setDatabase(MAIN_WIKI);

        // Search for all those documents with macro definitions.
        String sql =
            "select doc.fullName from XWikiDocument doc, BaseObject obj where doc.fullName=obj.name and obj.className=?";
        List<Object> wikiMacroDocs = null;
        try {
            wikiMacroDocs =
                xcontext.getWiki().getStore().search(sql, 0, 0, Arrays.asList("XWiki.WikiMacroClass"), xcontext);
        } catch (XWikiException ex) {
            getLogger().error("Error while searching for macro documents", ex);
            return;
        }

        // Build macros.
        Map<String, WikiMacro> wikiMacros = new HashMap<String, WikiMacro>();
        for (Object obj : wikiMacroDocs) {
            String wikiMacroDoc = (String) obj;
            try {
                WikiMacro macro = wikiMacroBuilder.buildMacro(wikiMacroDoc);
                wikiMacros.put(wikiMacroDoc, macro);
            } catch (WikiMacroBuilderException ex) {
                // Just log the exception and skip to the next.
                getLogger().error(ex.getMessage(), ex);
            }
        }

        // Register the wiki macros against WikiMacroManager.
        for (String documentName : wikiMacros.keySet()) {
            wikiMacroManager.registerWikiMacro(MAIN_WIKI + ":" + documentName, wikiMacros.get(documentName));
        }
    }
}
