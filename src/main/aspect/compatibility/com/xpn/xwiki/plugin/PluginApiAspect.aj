package com.xpn.xwiki.plugin;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.plugin.PluginApi} class.
 *
 * @version $Id: $
 */
public privileged aspect PluginApiAspect
{
    /**
     * @deprecated Replaced by {@link com.xpn.xwiki.plugin.PluginApi#getInternalPlugin()} since 1.3RC1.
     */
    public XWikiPluginInterface PluginApi.getPlugin()
    {
        return this.plugin;
    }

}
