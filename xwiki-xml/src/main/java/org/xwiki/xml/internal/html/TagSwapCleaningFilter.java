package org.xwiki.xml.internal.html;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;

import java.util.Iterator;

public class TagSwapCleaningFilter implements CleaningFilter
{
    public void filter(Document document)
    {
        swapTag(document, "b", "strong");
        swapTag(document, "i", "em");
    }

    protected void swapTag(Document document, String sourceTag, String newTag)
    {
        Iterator descendants = document.getDescendants(new ElementFilter(sourceTag));
        while (descendants.hasNext()) {
            Element element = (Element) descendants.next();
            element.setName(newTag);
        }
    }
}
