package org.xwiki.rendering.internal.macro.wikibridge;

import java.util.Map;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Initialize the binding provided to the script macros. Called before executing each wiki macro is executed.
 * 
 * @version $Id$
 * @since 2.5M1
 */
@ComponentRole
public interface WikiMacroBindingInitializer
{
    /**
     * Initialize the binding provided to the script macros.
     * 
     * @param macroDocumentReference the reference of the document containing the wiki macro
     * @param parameters the parameters of the macro
     * @param macroContent the content of the macro
     * @param context the macro execution context
     * @param macroBinding the binding map to fill
     */
    void initialize(DocumentReference macroDocumentReference, WikiMacroParameters parameters, String macroContent,
        MacroTransformationContext context, Map<String, Object> macroBinding);
}
