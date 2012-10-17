package org.xwiki.localization.message;

import org.xwiki.localization.internal.message.TranslationMessageElement;


public interface TranslationMessage extends TranslationMessageElement
{
    /**
     * @return the raw source of the translation as it is stored
     */
    String getRawSource();
}
