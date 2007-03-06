package com.xpn.xwiki.plugin.autotag;

/**
 * Created by IntelliJ IDEA.
 * User: jerem
 * Date: Nov 27, 2006
 * Time: 9:17:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class Tag implements Comparable{

    String name;
    long size;

    public Tag(String tagName, long tagSize) {
        this.size = tagSize;
        this.name = tagName;

    }

    public int compareTo(Object o) {
        if (o instanceof Tag)
            return - ((Tag)o).name.compareTo(name);
        return 0;
    }


    public String getHtml(){
        return "<a class=\"f" + size + "\">" + name + "</a> ";
    }
}
