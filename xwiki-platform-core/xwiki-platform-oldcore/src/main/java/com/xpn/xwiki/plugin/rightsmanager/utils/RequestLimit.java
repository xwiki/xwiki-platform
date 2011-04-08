package com.xpn.xwiki.plugin.rightsmanager.utils;

/**
 * Contains maximum number of result to return and index of the first element.
 * 
 * @version $Id$
 * @since XWiki Core 1.1.2, XWiki Core 1.2M2
 */
public class RequestLimit
{
    /**
     * The maximum number of result to return.
     */
    private int nb;

    /**
     * The index of the first found element to return.
     */
    private int start;

    /**
     * Construct new instance of RequestLimit with provided nb and start.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found element to return.
     */
    public RequestLimit(int nb, int start)
    {
        this.setNb(nb);
        this.setStart(start);
    }

    /**
     * @param nb the maximum number of result to return.
     */
    public void setNb(int nb)
    {
        this.nb = nb;
    }

    /**
     * @return the maximum number of result to return.
     */
    public int getNb()
    {
        return this.nb;
    }

    /**
     * @param start the index of the first found element to return.
     */
    public void setStart(int start)
    {
        this.start = start;
    }

    /**
     * @return the index of the first found element to return.
     */
    public int getStart()
    {
        return this.start;
    }
}
