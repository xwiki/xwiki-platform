package org.xwiki.xml.internal.html;

import org.jdom.Document;

public interface CleaningFilter
{
    void filter(Document document);
}
