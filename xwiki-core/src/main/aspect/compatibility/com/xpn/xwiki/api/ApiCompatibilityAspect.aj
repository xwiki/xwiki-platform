package compatibility.com.xpn.xwiki.api;

import com.xpn.xwiki.api.Api;

/**
 * Add a backward compatibility layer to the {@link Api} class.
 * 
 * @version $Id: $
 */
public privileged aspect ApiCompatibilityAspect
{
    /**
     * @return true if the current user has the Programming right or false otherwise
     * @deprecated use {@link Api#hasProgrammingRights()} instead
     */
    public boolean Api.checkProgrammingRights()
    {
        return this.hasProgrammingRights();
    }
}
