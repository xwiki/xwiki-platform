package com.xpn.xwiki.test;

import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.render.XWikiWikiBaseRenderer;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 8 mars 2004
 * Time: 09:22:34
 * To change this template use File | Settings | File Templates.
 */
public class WikiWikiBaseRenderTest extends RenderTest {

    public XWikiRenderer getXWikiRenderer() {
        return new XWikiWikiBaseRenderer();
    }
}
