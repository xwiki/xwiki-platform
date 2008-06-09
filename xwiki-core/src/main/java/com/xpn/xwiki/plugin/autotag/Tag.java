package com.xpn.xwiki.plugin.autotag;

public class Tag implements Comparable
{

    String name;

    long size;

    public Tag(String tagName, long tagSize)
    {
        this.size = tagSize;
        this.name = tagName;

    }

    public int compareTo(Object o)
    {
        if (o instanceof Tag)
            return -((Tag) o).name.compareTo(name);
        return 0;
    }

    public String getHtml()
    {
        return "<a class=\"f" + size + "\">" + name + "</a> ";
    }
}
