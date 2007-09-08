/**
 * 
 */
package com.xpn.xwiki.xmlrpc.model.swizzle;

import java.util.Map;

import com.xpn.xwiki.xmlrpc.model.Permission;

/**
 * @author hritcu
 *
 */
public class PermissionImpl implements Permission
{
    private org.codehaus.swizzle.confluence.Permission target;
    
    public PermissionImpl()
    {
        target = new org.codehaus.swizzle.confluence.Permission();
    }

    public PermissionImpl(Map map)
    {
        target = new org.codehaus.swizzle.confluence.Permission(map);
    }
    
    public PermissionImpl(org.codehaus.swizzle.confluence.Permission permission)
    {
        target = permission;
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Permission#getLockType()
     */
    public String getLockType()
    {
        return target.getLockType();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Permission#getLockedBy()
     */
    public String getLockedBy()
    {
        return target.getLockedBy();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Permission#setLockType(java.lang.String)
     */
    public void setLockType(String lockType)
    {
        target.setLockType(lockType);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Permission#setLockedBy(java.lang.String)
     */
    public void setLockedBy(String lockedBy)
    {
        target.setLockedBy(lockedBy);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.MapObject#toMap()
     */
    public Map toMap()
    {
        return target.toMap();
    }
}
