package org.xwiki.extension.xar.internal.handler.packager;

import com.xpn.xwiki.doc.merge.MergeResult;

public class XarEntryMergeResult
{
    private XarEntry entry;

    private MergeResult result;

    public XarEntryMergeResult(XarEntry entry, MergeResult result)
    {
        this.entry = entry;
        this.result = result;
    }

    public XarEntry getEntry()
    {
        return this.entry;
    }

    public MergeResult getResult()
    {
        return this.result;
    }
}
