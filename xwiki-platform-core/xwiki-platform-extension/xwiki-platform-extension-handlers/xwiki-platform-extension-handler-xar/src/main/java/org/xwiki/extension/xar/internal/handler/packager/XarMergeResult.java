package org.xwiki.extension.xar.internal.handler.packager;

import java.util.LinkedHashMap;
import java.util.Map;

public class XarMergeResult
{
    private Map<XarEntry, XarEntryMergeResult> mergeResults = new LinkedHashMap<XarEntry, XarEntryMergeResult>();

    public Map<XarEntry, XarEntryMergeResult> getMergeResults()
    {
        return this.mergeResults;
    }

    public void addMergeResult(XarEntryMergeResult mergeResult)
    {
        this.mergeResults.put(mergeResult.getEntry(), mergeResult);
    }
}
