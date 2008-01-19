package com.xpn.xwiki.plugin.globalsearch.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Contains one query result line.
 * 
 * @version $Id: $
 */
public class GlobalSearchResult extends HashMap
{
    /**
     * The name of the wiki where this was found.
     */
    private String wikiName;

    /**
     * The found values.
     */
    private Collection result;

    /**
     * Create new {@link GlobalSearchResult} instance.
     */
    public GlobalSearchResult()
    {

    }

    /**
     * Create new {@link GlobalSearchResult} instance.
     * 
     * @param wikiName the name of the wiki where this was found.
     * @param names the names of the columns to link with values.
     * @param values the found values.
     */
    public GlobalSearchResult(String wikiName, Iterable names, Object[] values)
    {
        setWikiName(wikiName);

        this.result = new ArrayList(values.length);
        Iterator nameIt = names.iterator();
        for (int i = 0; i < values.length; ++i) {
            Object value = values[i];

            this.result.add(value);
            put(nameIt.next(), value);
        }
    }

    /**
     * @param wikiName the name of the wiki where this was found.
     */
    public void setWikiName(String wikiName)
    {
        this.wikiName = wikiName;
    }

    /**
     * @return the name of the wiki where this was found.
     */
    public String getWikiName()
    {
        return wikiName;
    }

    /**
     * @param result the found values.
     */
    public void setResult(Collection result)
    {
        this.result = new ArrayList(result);
    }

    /**
     * @param result the found values.
     */
    public void setResult(Object[] result)
    {
        this.result = Arrays.asList(result);
    }

    /**
     * @return the found values.
     */
    public Collection getResult()
    {
        return result;
    }
}
