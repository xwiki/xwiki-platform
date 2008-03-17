package compatibility.com.xpn.xwiki;

import com.xpn.xwiki.XWikiContext;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.XWikiContext} class.
 * 
 * @version $Id: $
 */
public privileged aspect XWikiContextCompatibilityAspect
{
    /**
     * @return true it's main wiki's context, false otherwise.
     * @deprecated replaced by {@link XWikiContext#isMainWiki()} since 1.4M1.
     */
    public boolean XWikiContext.isVirtual()
    {
        return !this.isMainWiki();
    }

    /**
     * @param virtual true it's main wiki's context, false otherwise.
     * @deprecated this methods is now useless because the virtuality of a wiki is resolved with a
     *             comparison between {@link XWikiContext#getDatabase()} and
     *             {@link XWikiContext#getMainXWiki()} since 1.4M1.
     */
    public void XWikiContext.setVirtual(boolean virtual)
    {
        // this.virtual = virtual;
    }
}
