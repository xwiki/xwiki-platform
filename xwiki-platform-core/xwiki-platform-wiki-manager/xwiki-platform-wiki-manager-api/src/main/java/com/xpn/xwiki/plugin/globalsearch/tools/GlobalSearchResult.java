package com.xpn.xwiki.plugin.globalsearch.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Contains one query result line.
 * 
 * @version $Id$
 */
public class GlobalSearchResult extends HashMap<String, Object>
{
    /**
     * The name of the wiki where this was found.
     */
    private String wikiName;

    /**
     * The found values.
     */
    private Collection<Object> result;

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
    public GlobalSearchResult(String wikiName, Iterable<String> names, Object[] values)
    {
        setWikiName(wikiName);

        this.result = new ArrayList<Object>(values.length);

        Iterator<String> nameIt = names.iterator();
        for (Object value : values) {
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
    public void setResult(Collection< ? > result)
    {
        this.result = new ArrayList<Object>(result);
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
    public Collection<Object> getResult()
    {
        return result;
    }
}
