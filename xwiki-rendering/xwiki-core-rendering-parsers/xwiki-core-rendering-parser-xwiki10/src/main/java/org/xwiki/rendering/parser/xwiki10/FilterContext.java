package org.xwiki.rendering.parser.xwiki10;

import java.util.LinkedList;
import java.util.List;

public class FilterContext
{
    public static final String XWIKI1020TOKEN = "XWIKI1020TOKEN";

    private List<String> protectedContentList = new LinkedList<String>();

    public String addProtectedContent(String content)
    {
        this.protectedContentList.add(content);

        return "{" + XWIKI1020TOKEN + (this.protectedContentList.size() - 1) + "}";
    }

    public void setProtectedContent(int index, String content)
    {
        this.protectedContentList.set(index, content);
    }

    public String getProtectedContent(int index)
    {
        return this.protectedContentList.get(index);
    }
}
