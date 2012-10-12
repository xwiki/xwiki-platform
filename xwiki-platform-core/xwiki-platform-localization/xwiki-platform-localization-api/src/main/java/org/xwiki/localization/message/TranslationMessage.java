package org.xwiki.localization.message;


public interface TranslationMessage extends TranslationMessageElement
{
    /**
     * @return the raw source of the translation as it is stored
     */
    String getRawSource();
}
