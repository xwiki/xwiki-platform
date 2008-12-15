package com.xpn.xwiki.plugin.webdav.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jackrabbit.webdav.property.DavPropertySet;

import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;

public class XWikiDavUserStorage
{
    /**
     * Class version
     */
    private static final long serialVersionUID = -2125494700535339990L;
    
    /**
     * In-memory resources map for this particular user cache.
     */
    private Map<String, List<XWikiDavResource>> resourcesMap;
    
    /**
     * In-memory properties map for this particular user cache.
     */
    private Map<String, DavPropertySet> propertiesMap;
    
    /**
     * Default constructor.
     */
    public XWikiDavUserStorage(){
        resourcesMap = new HashMap<String, List<XWikiDavResource>>();
        propertiesMap = new HashMap<String, DavPropertySet>();
    }

    public Map<String, List<XWikiDavResource>> getResourcesMap()
    {
        return resourcesMap;
    }

    public Map<String, DavPropertySet> getPropertiesMap()
    {
        return propertiesMap;
    }    
}
