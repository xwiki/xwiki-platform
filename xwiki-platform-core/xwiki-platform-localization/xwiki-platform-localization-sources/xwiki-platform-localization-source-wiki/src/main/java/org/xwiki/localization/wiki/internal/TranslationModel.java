package org.xwiki.localization.wiki.internal;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

public interface TranslationModel
{
    String TRANSLATIONCLASS_REFERENCE_SPACE = "XWiki";

    String TRANSLATIONCLASS_REFERENCE_NAME = "TranslationDocumentClass";

    EntityReference TRANSLATIONCLASS_REFERENCE = new EntityReference(TRANSLATIONCLASS_REFERENCE_NAME,
        EntityType.DOCUMENT, new EntityReference(TRANSLATIONCLASS_REFERENCE_SPACE, EntityType.SPACE));

    String TRANSLATIONCLASS_REFERENCE_STRING = TRANSLATIONCLASS_REFERENCE_SPACE + '.' + TRANSLATIONCLASS_REFERENCE_NAME;

    String TRANSLATIONCLASS_PROP_SCOPE = "scope";

    enum Scope
    {
        GLOBAL,
        WIKI,
        USER
    }
}
