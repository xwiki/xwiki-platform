package org.xwiki.wikistream.internal;

import java.util.Collections;
import java.util.Map;

public class PropertiesTree
{
    private final PropertiesTree parent;

    private final Map<String, Object> properties;

    public PropertiesTree(Map<String, Object> properties, PropertiesTree parent)
    {
        this.properties = properties != null ? properties : Collections.<String, Object> emptyMap();
        this.parent = parent;
    }

    public PropertiesTree getParent()
    {
        return this.parent;
    }

    public Map<String, Object> getProperties()
    {
        return this.properties;
    }

    public <T> T get(String key)
    {
        return get(key, null);
    }

    public <T> T get(String key, T def)
    {
        if (getProperties().containsKey(key)) {
            return (T) this.properties.get(key);
        }

        if (getParent() != null) {
            return getParent().get(key, def);
        }

        return def;
    }
}
