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
package com.xpn.xwiki.internal.macro;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.internal.macro.wikibridge.WikiMacroBindingInitializer;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Provide old core related wiki macro bindings.
 * 
 * @version $Id$
 * @since 2.5M1
 */
@Component
@Singleton
@Named("core")
public class CoreWikiMacroBindingInitializer implements WikiMacroBindingInitializer
{
    /**
     * The key under which macro can access the document where it's defined. Same as CONTEXT_DOCUMENT_KEY (Check style
     * fix).
     */
    private static final String MACRO_DOC_KEY = "doc";

    /**
     * Execution context handler, needed for accessing the XWikiContext.
     */
    @Inject
    private Execution execution;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * @return the XWiki context
     */
    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.macro.wikibridge.WikiMacroBindingInitializer#initialize(org.xwiki.model.reference.DocumentReference,
     *      org.xwiki.rendering.macro.wikibridge.WikiMacroParameters, java.lang.String,
     *      org.xwiki.rendering.transformation.MacroTransformationContext, java.util.Map)
     */
    public void initialize(DocumentReference macroDocumentReference, WikiMacroParameters parameters,
        String macroContent, MacroTransformationContext context, Map<String, Object> macroBinding)
    {
        try {
            XWikiDocument document = getContext().getWiki().getDocument(macroDocumentReference, getContext());
            macroBinding.put(MACRO_DOC_KEY, document.newDocument(getContext()));
        } catch (XWikiException e) {
            this.logger.error("Failed to get document " + macroDocumentReference, e);
        }
    }
}
