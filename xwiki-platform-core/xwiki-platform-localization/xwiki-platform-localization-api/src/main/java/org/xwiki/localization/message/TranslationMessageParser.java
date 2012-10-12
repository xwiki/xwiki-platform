package org.xwiki.localization.message;

import org.xwiki.component.annotation.Role;

@Role
public interface TranslationMessageParser
{
    TranslationMessage parse(String translationMessage);
}
