/**
 * 
 */
package com.xpn.xwiki.xmlrpc.model.swizzle;

import java.util.Map;

import com.xpn.xwiki.xmlrpc.model.BlogEntry;

/**
 * @author hritcu
 *
 */
public class BlogEntryImpl implements BlogEntry
{
    
    private org.codehaus.swizzle.confluence.BlogEntry target;
    
    public BlogEntryImpl()
    {
        target = new org.codehaus.swizzle.confluence.BlogEntry();
    }

    public BlogEntryImpl(Map map)
    {
        target = new org.codehaus.swizzle.confluence.BlogEntry(map);
    }

    public BlogEntryImpl(org.codehaus.swizzle.confluence.BlogEntry blogEntry)
    {
        target = blogEntry;
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntry#getContent()
     */
    public String getContent()
    {
        return target.getContent();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntry#getId()
     */
    public String getId()
    {
        return target.getId();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntry#getLocks()
     */
    public int getLocks()
    {
        return target.getLocks();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntry#getSpace()
     */
    public String getSpace()
    {
        return target.getSpace();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntry#getTitle()
     */
    public String getTitle()
    {
        return target.getTitle();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntry#getUrl()
     */
    public String getUrl()
    {
        return target.getUrl();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntry#getVersion()
     */
    public int getVersion()
    {
        return target.getVersion();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntry#setContent(java.lang.String)
     */
    public void setContent(String content)
    {
        target.setContent(content);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntry#setId(java.lang.String)
     */
    public void setId(String id)
    {
        target.setId(id);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntry#setLocks(int)
     */
    public void setLocks(int locks)
    {
        target.setLocks(locks);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntry#setSpace(java.lang.String)
     */
    public void setSpace(String space)
    {
        target.setSpace(space);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntry#setTitle(java.lang.String)
     */
    public void setTitle(String title)
    {
        target.setTitle(title);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntry#setUrl(java.lang.String)
     */
    public void setUrl(String url)
    {
        target.setUrl(url);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.BlogEntry#setVersion(int)
     */
    public void setVersion(int version)
    {
        target.setVersion(version);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.MapObject#toMap()
     */
    public Map toMap()
    {
        return target.toMap();
    }

    public org.codehaus.swizzle.confluence.BlogEntry getTarget()
    {
        return target;
    }
}
