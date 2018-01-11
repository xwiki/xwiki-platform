package com.xpn.xwiki.doc;

import java.util.ArrayList;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class DecoratedList extends ArrayList<XWikiAttachment>
{

    private Map<String, XWikiAttachment> map;

    public DecoratedList()
    {
        map = new TreeMap<String, XWikiAttachment>();
    }

    @Override

    public boolean add(XWikiAttachment attachment)
    {
        map.put(attachment.getFilename(), attachment);
        super.clear();
        super.addAll(map.values());
        return true;
    }

    @Override

    public void clear()
    {
        super.clear();
        map.clear();
    }

    @Override

    public boolean addAll(Collection<? extends XWikiAttachment> c)
    {
        for (XWikiAttachment x : c) {
            map.put(x.getFilename(), x);
        }
        super.clear();
        super.addAll(map.values());
        return true;

    }

    public XWikiAttachment remove(int index)
    {

        XWikiAttachment removedAttachment = map.remove(super.get(index).getFilename());
        return removedAttachment == null ? null : super.remove(index);

    }

    public XWikiAttachment remove(XWikiAttachment x)
    {

        String filename = x.getFilename();
        XWikiAttachment removedAttachment = map.remove(filename);
        super.clear();
        super.addAll(map.values());
        return removedAttachment;

    }

    public XWikiAttachment set(XWikiAttachment x)
    {

        map.put(x.getFilename(), x);
        super.clear();
        super.addAll(map.values());
        return x;

    }

    public XWikiAttachment getByFilename(String filename)
    {
        return map.get(filename);
    }

}
