package com.xpn.xwiki.internal.skin;

import org.xwiki.filter.input.InputSource;

public abstract class AbstractSkin implements Skin
{
    protected String id;

    protected Skin parent;

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public Skin getParent()
    {
        return this.parent;
    }

    @Override
    public InputSource getResourceInputSource(String resource)
    {
        InputSource inputSource = getSkinResourceInputSource(resource);

        return inputSource != null ? inputSource : getParent().getResourceInputSource(resource);
    }
}
