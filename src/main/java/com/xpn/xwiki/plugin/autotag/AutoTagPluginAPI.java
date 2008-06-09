package com.xpn.xwiki.plugin.autotag;

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.XWikiContext;

public class AutoTagPluginAPI extends Api
{

    private AutoTagPlugin plugin;

    public AutoTagPluginAPI(XWikiContext context)
    {
        super(context);
    }

    public AutoTagPluginAPI(AutoTagPlugin autoTagPlugin, XWikiContext context)
    {
        super(context);
        this.plugin = autoTagPlugin;
    }

    public TagCloud generateTagCloud(String text, int lang)
    {
        return plugin.generateTagCloud(text, lang);
    }
    
    public TagCloud generateTagCloud(String text, String lang) 
    {
        return plugin.generateTagCloud(text, plugin.getLanguageConstant(lang));
    }
}
