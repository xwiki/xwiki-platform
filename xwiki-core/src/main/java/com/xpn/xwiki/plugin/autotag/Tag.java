package com.xpn.xwiki.plugin.autotag;

public class Tag implements Comparable<Tag>
{
    String name;

    long size;

    public Tag(String tagName, long tagSize)
    {
        this.size = tagSize;
        this.name = tagName;
    }

    public int compareTo(Tag o)
    {
        return -o.name.compareTo(this.name);
    }

    public String getHtml()
    {
        return "<a class=\"f" + this.size + "\">" + this.name + "</a> ";
    }
}
