package com.xpn.xwiki.plugin.globalsearch.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Contains all parameters for a global search query.
 * 
 * @version $Id$
 */
public class GlobalSearchQuery
{
    /**
     * The names of wikis where to search.
     */
    private List<String> wikiNameList = new ArrayList<String>();

    /**
     * The hql query.
     */
    private String hql;

    /**
     * The values to insert in the named query.
     */
    private List<Object> parameterList = new ArrayList<Object>();

    /**
     * The maximum number of results.
     */
    private int max;

    /**
     * The first index of the result.
     */
    private int start;

    /**
     * @param wikiNameList names of wikis where to search.
     */
    public void addWikiNameList(Collection<String> wikiNameList)
    {
        this.wikiNameList.addAll(wikiNameList);
    }

    /**
     * @param wikiNameList the names of wikis where to search.
     */
    public void setWikiNameList(Collection<String> wikiNameList)
    {
        this.wikiNameList.clear();
        if (wikiNameList != null) {
            this.addWikiNameList(wikiNameList);
        }
    }

    /**
     * @param wikiName a name of wiki where to search.
     */
    public void addWikiName(String wikiName)
    {
        this.wikiNameList.add(wikiName);
    }

    /**
     * @return the names of wikis where to search.
     */
    public Collection<String> getWikiNameList()
    {
        return this.wikiNameList;
    }

    /**
     * @param hql the hql query.
     */
    public void setHql(String hql)
    {
        this.hql = hql;
    }

    /**
     * @return the hql query.
     */
    public String getHql()
    {
        return this.hql;
    }

    /**
     * @param values values to insert in the named query.
     */
    public void addParameterList(Collection< ? > values)
    {
        this.parameterList.addAll(values);
    }

    /**
     * @param values the values to insert in the named query.
     */
    public void setParameterList(Collection< ? > values)
    {
        this.parameterList.clear();
        this.parameterList.addAll(values);
    }

    /**
     * @param value a value to insert in the named query.
     */
    public void addParameter(Object value)
    {
        this.parameterList.add(value);
    }

    /**
     * @return the values to insert in the named query.
     */
    public List<Object> getParameterList()
    {
        return this.parameterList;
    }

    /**
     * @param max the maximum number of results.
     */
    public void setMax(int max)
    {
        this.max = max;
    }

    /**
     * @return the maximum number of results.
     */
    public int getMax()
    {
        return this.max;
    }

    /**
     * @param start the index of the first result.
     */
    public void setStart(int start)
    {
        this.start = start;
    }

    /**
     * @return the index of the first result.
     */
    public int getStart()
    {
        return this.start;
    }
}
