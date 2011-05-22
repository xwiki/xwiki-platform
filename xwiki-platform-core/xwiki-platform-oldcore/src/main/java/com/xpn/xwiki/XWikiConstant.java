package com.xpn.xwiki;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

public class XWikiConstant
{
    public static final String TAG_CLASS = "XWiki.TagClass";

    public static final String TAG_CLASS_PROP_TAGS = "tags";

    /**
     * @deprecated since 3.1M2, use {@link #EDIT_MODE_CLASS} constant and class instead
     */
    public static final String SHEET_CLASS = "XWiki.SheetClass";

    /**
     * The class that holds the default edit mode for a document. The object of this class can be attached either to the
     * document itself or to an included sheet. If both are found, the one attached to the document is used.
     * 
     * @since 3.1M2
     */
    public static final EntityReference EDIT_MODE_CLASS = new EntityReference("EditModeClass", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));
}
