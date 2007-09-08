/**
 * 
 */
package com.xpn.xwiki.xmlrpc.model.swizzle;

import java.util.Map;

import com.xpn.xwiki.xmlrpc.model.Label;

/**
 * @author hritcu
 *
 */
public class LabelImpl implements Label
{
    
    private org.codehaus.swizzle.confluence.Label target;
    
    public LabelImpl()
    {
        target = new org.codehaus.swizzle.confluence.Label();
    }

    public LabelImpl(Map map)
    {
        target = new org.codehaus.swizzle.confluence.Label(map);
    }
    
    public LabelImpl(org.codehaus.swizzle.confluence.Label label)
    {
        target = label;
    }
    
    /**
     * @see com.xpn.xwiki.xmlrpc.model.Label#getId()
     */
    public String getId()
    {
        return target.getId();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Label#getName()
     */
    public String getName()
    {
        return target.getName();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Label#getNamespace()
     */
    public String getNamespace()
    {
        return target.getNamespace();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Label#getOwner()
     */
    public String getOwner()
    {
        return target.getOwner();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Label#setId(java.lang.String)
     */
    public void setId(String id)
    {
        target.setId(id);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Label#setName(java.lang.String)
     */
    public void setName(String name)
    {
        target.setName(name);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Label#setNamespace(java.lang.String)
     */
    public void setNamespace(String namespace)
    {
        target.setNamespace(namespace);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Label#setOwner(java.lang.String)
     */
    public void setOwner(String owner)
    {
        target.setOwner(owner);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.MapObject#toMap()
     */
    public Map toMap()
    {
        return target.toMap();
    }

    public org.codehaus.swizzle.confluence.Label getTarget()
    {
        return target;
    }
}
