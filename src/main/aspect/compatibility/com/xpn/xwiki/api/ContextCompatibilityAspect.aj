package compatibility.com.xpn.xwiki.api;

import com.xpn.xwiki.api.Context;

/**
 * Add a backward compatibility layer to the {@link Context} class.
 * 
 * @version $Id: $
 */
public privileged aspect ContextCompatibilityAspect
{
    /**
     * @return true it's main wiki's context, false otherwise.
     * @deprecated replaced by {@link Context#isMainWiki()} since 1.4M1.
     */
    public boolean Context.isVirtual()
    {
        return !this.isMainWiki();
    }
}
