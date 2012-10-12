package com.xpn.xwiki.internal.localization;

import java.text.MessageFormat;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.localization.message.TranslationMessageParser;

@Component
@Singleton
@Named("xwikil10n/1.0")
public class XWiki10TranslationMessageParser implements TranslationMessageParser
{
    @Override
    public TranslationMessage parse(String translationMessage)
    {
        MessageFormat messageFormat = new MessageFormat(translationMessage);
        
        messageFormat.getFormats();
        
        MessageFormat.format(translationMessage, params);
        // TODO Auto-generated method stub
        return null;
    }
}
