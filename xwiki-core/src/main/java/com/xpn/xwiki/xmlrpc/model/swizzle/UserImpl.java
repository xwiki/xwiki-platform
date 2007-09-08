/**
 * 
 */
package com.xpn.xwiki.xmlrpc.model.swizzle;

import java.util.Map;

import org.codehaus.swizzle.confluence.MapConvertor;
import org.codehaus.swizzle.confluence.SwizzleConversionException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.xmlrpc.model.User;

/**
 * @author hritcu
 *
 */
public class UserImpl implements User
{
    private org.codehaus.swizzle.confluence.User target;
    
    public UserImpl()
    {
        target = new org.codehaus.swizzle.confluence.User();
    }

    public UserImpl(Map map)
    {
        target = new org.codehaus.swizzle.confluence.User(map);
    }
    
    public UserImpl(Map map, MapConvertor convertor) throws XWikiException
    {
        Map typeMap = org.codehaus.swizzle.confluence.User.FIELD_TYPES;
        try {
            target = new org.codehaus.swizzle.confluence.User(convertor.revert(map, typeMap));
        } catch (SwizzleConversionException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_XMLRPC, 0, e.getMessage(), e);
        }
    }

    public UserImpl(org.codehaus.swizzle.confluence.User user)
    {
        target = user;
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.User#getEmail()
     */
    public String getEmail()
    {
        return target.getEmail();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.User#getFullname()
     */
    public String getFullname()
    {
        return target.getFullname();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.User#getName()
     */
    public String getName()
    {
        return getName();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.User#getUrl()
     */
    public String getUrl()
    {
        return getUrl();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.User#setEmail(java.lang.String)
     */
    public void setEmail(String email)
    {
        target.setEmail(email);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.User#setFullname(java.lang.String)
     */
    public void setFullname(String fullname)
    {
        target.setFullname(fullname);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.User#setName(java.lang.String)
     */
    public void setName(String name)
    {
        target.setName(name);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.User#setUrl(java.lang.String)
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

    public org.codehaus.swizzle.confluence.User getTarget()
    {
        return target;
    }
}
