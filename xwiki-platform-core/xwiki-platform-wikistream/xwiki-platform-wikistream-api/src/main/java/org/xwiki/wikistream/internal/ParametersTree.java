package org.xwiki.wikistream.internal;

import java.util.Map;

import org.xwiki.filter.FilterEventParameters;

public class ParametersTree
{
    private final ParametersTree parent;

    private final FilterEventParameters properties;

    public ParametersTree(FilterEventParameters properties, ParametersTree parent)
    {
        this.properties = properties != null ? properties : FilterEventParameters.EMPTY;
        this.parent = parent;
    }

    public ParametersTree getParent()
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

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        builder.append(this.properties.toString());

        if (this.parent != null) {
            builder.append(" -> ");
            builder.append(this.parent);
        }

        return builder.toString();
    }
}
