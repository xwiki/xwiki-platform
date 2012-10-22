package org.xwiki.localization.internal;

import java.util.Locale;

import org.xwiki.model.reference.DocumentReference;

public class DocumentBundle extends AbstractWikiDocumentBundle
{
    public DocumentBundle(DocumentReference reference)
    {
        this(reference, null);
    }

    public DocumentBundle(DocumentReference reference, String syntax)
    {
        
    }

    @Override
    protected LocaleBundle createBundle(Locale locale)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
