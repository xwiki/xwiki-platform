package org.xwiki.rendering.wikimacro.internal;

/**
 * Legacy aspect for {@link WikiMacroConstants}.
 * 
 * @version $Id$
 * @since 14.6RC1
 */
public aspect WikiMacroConstantsAspect
{
    /**
     * Constant for representing default macro category property.
     * @deprecated since 14.6RC1, replaced by {@link WikiMacroConstants#MACRO_DEFAULT_CATEGORIES_PROPERTY}
     */
    @Deprecated(since = "14.6RC1")
    public String WikiMacroConstantsAspect.MACRO_DEFAULT_CATEGORY_PROPERTY = "defaultCategory";
}
