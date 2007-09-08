/**
 * 
 */
package com.xpn.xwiki.xmlrpc.model.swizzle;

import java.util.Date;
import java.util.Map;

import com.xpn.xwiki.xmlrpc.model.BlogEntrySummary;

/**
 * @author hritcu
 *
 */
public class BlogEntrySummaryImpl implements BlogEntrySummary
{
    private org.codehaus.swizzle.confluence.BlogEntrySummary target;
    
    public BlogEntrySummaryImpl()
    {
        target = new org.codehaus.swizzle.confluence.BlogEntrySummary();
    }

    public BlogEntrySummaryImpl(Map map)
    {
        target = new org.codehaus.swizzle.confluence.BlogEntrySummary(map);
    }
    
    public BlogEntrySummaryImpl(org.codehaus.swizzle.confluence.BlogEntrySummary blogEntrySummary)
    {
        target = blogEntrySummary;
    }
    
    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntrySummary#getId()
     */
    public String getId()
    {
        return target.getId();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntrySummary#getLocks()
     */
    public int getLocks()
    {
        return target.getLocks();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntrySummary#getPublishDate()
     */
    public Date getPublishDate()
    {
        return target.getPublishDate();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntrySummary#getSpace()
     */
    public String getSpace()
    {
        return target.getSpace();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntrySummary#getTitle()
     */
    public String getTitle()
    {
        return target.getTitle();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntrySummary#getUrl()
     */
    public String getUrl()
    {
        return target.getUrl();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntrySummary#setId(java.lang.String)
     */
    public void setId(String id)
    {
        target.setId(id);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntrySummary#setLocks(int)
     */
    public void setLocks(int locks)
    {
        target.setLocks(locks);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntrySummary#setPublishDate(java.util.Date)
     */
    public void setPublishDate(Date publishDate)
    {
        target.setPublishDate(publishDate);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntrySummary#setSpace(java.lang.String)
     */
    public void setSpace(String space)
    {
        target.setSpace(space);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntrySummary#setTitle(java.lang.String)
     */
    public void setTitle(String title)
    {
        target.setTitle(title);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntrySummary#setUrl(java.lang.String)
     */
    public void setUrl(String url)
    {
        target.setUrl(url);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.MapObject#toMap()
     */
    public Map toMap()
    {
        return target.toMap();
    }
}
