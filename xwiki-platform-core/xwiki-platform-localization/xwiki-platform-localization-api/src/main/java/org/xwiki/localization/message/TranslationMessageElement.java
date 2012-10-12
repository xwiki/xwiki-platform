package org.xwiki.localization.message;

import java.util.Collection;
import java.util.Locale;

import org.xwiki.localization.Bundle;
import org.xwiki.rendering.block.Block;

public interface TranslationMessageElement
{
    /**
     * Execute the transformation (resolve any variable or parameter in its content) and produce a Block to insert in an
     * into a XDOM or to render as it is.
     * 
     * @param locale the locale to used to resolve variables
     * @param bundles the bundles to resolve variables with
     * @param parameters the parameters
     * @return the result translation
     */
    Block render(Locale locale, Collection<Bundle> bundles, Object... parameters);
}
