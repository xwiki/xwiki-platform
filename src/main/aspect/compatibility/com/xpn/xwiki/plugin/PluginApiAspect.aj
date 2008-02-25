package com.xpn.xwiki.plugin;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.plugin.PluginApi} class.
 *
 * @version $Id: $
 */
public privileged aspect PluginApiAspect
{
    /**
     * Add a backward compatibility layer to the {@link com.xpn.xwiki.XWiki} class.
     *
     * @version $Id: $
     */
    public XWikiPluginInterface PluginApi.getPlugin()
    {
        return this.plugin;
    }

}
