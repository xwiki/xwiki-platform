package org.xwiki.rendering.macro.rss;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.internal.macro.rss.RssMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

public class TestRssMacro2 extends RssMacro
{
    
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(RssMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        RssMacroParameters rssParameters = new RssMacroParameters();
        rssParameters.setFull(true);
        rssParameters.setCss(true);
        rssParameters.setCount(1);
        rssParameters.setImage(false);
        
        URL feedURL = getClass().getResource("/feed2.xml");
        try {
            rssParameters.setFeed(feedURL.toString());
        } catch (MalformedURLException e) {
            throw new MacroExecutionException(e.getMessage());
        }

        return super.execute(rssParameters, content, context);
    }

    /** 
     * {@inheritDoc}
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
    public boolean supportsInlineMode()
    {
        return false;
    }
}
