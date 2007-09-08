/**
 * 
 */
package com.xpn.xwiki.xmlrpc.model.swizzle;

import java.util.Date;
import java.util.Map;

import com.xpn.xwiki.xmlrpc.model.UserInformation;

/**
 * @author hritcu
 *
 */
public class UserInformationImpl implements UserInformation
{
    private org.codehaus.swizzle.confluence.UserInformation target;
    
    public UserInformationImpl()
    {
        target = new org.codehaus.swizzle.confluence.UserInformation();
    }

    public UserInformationImpl(Map map)
    {
        target = new org.codehaus.swizzle.confluence.UserInformation(map);
    }

    public UserInformationImpl(org.codehaus.swizzle.confluence.UserInformation userInformation)
    {
        target = userInformation;
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.UserInformation#getContent()
     */
    public String getContent()
    {
        return target.getContent();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.UserInformation#getCreationDate()
     */
    public Date getCreationDate()
    {
        return target.getCreationDate();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.UserInformation#getCreatorName()
     */
    public String getCreatorName()
    {
        return target.getCreatorName();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.UserInformation#getId()
     */
    public String getId()
    {
        return target.getId();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.UserInformation#getLastModificationDate()
     */
    public Date getLastModificationDate()
    {
        return target.getLastModificationDate();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.UserInformation#getLastModifierName()
     */
    public String getLastModifierName()
    {
        return target.getLastModifierName();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.UserInformation#getUsername()
     */
    public String getUsername()
    {
        return target.getUsername();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.UserInformation#getVersion()
     */
    public int getVersion()
    {
        return target.getVersion();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.UserInformation#setContent(java.lang.String)
     */
    public void setContent(String content)
    {
        target.setContent(content);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.UserInformation#setCreationDate(java.util.Date)
     */
    public void setCreationDate(Date creationDate)
    {
        target.setCreationDate(creationDate);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.UserInformation#setCreatorName(java.lang.String)
     */
    public void setCreatorName(String creatorName)
    {
        target.setCreatorName(creatorName);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.UserInformation#setId(java.lang.String)
     */
    public void setId(String id)
    {
        target.setId(id);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.UserInformation#setLastModificationDate(java.util.Date)
     */
    public void setLastModificationDate(Date lastModificationDate)
    {
        target.setLastModificationDate(lastModificationDate);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.UserInformation#setLastModifierName(java.lang.String)
     */
    public void setLastModifierName(String lastModifierName)
    {
        target.setLastModifierName(lastModifierName);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.UserInformation#setUsername(java.lang.String)
     */
    public void setUsername(String username)
    {
        target.setUsername(username);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.UserInformation#setVersion(int)
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

    public org.codehaus.swizzle.confluence.UserInformation getTarget()
    {
        return target;
    }

}
