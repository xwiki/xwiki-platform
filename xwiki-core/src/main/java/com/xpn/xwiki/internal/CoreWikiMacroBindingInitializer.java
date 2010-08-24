package com.xpn.xwiki.internal;

import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
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
@Component("core")
public class CoreWikiMacroBindingInitializer extends AbstractLogEnabled implements WikiMacroBindingInitializer
{
    /**
     * The key under which macro can access the document where it's defined. Same as CONTEXT_DOCUMENT_KEY (Check style
     * fix).
     */
    private static final String MACRO_DOC_KEY = "doc";

    /**
     * Execution context handler, needed for accessing the XWikiContext.
     */
    @Requirement
    private Execution execution;

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
            getLogger().error("Failed to get document " + macroDocumentReference, e);
        }
    }
}
