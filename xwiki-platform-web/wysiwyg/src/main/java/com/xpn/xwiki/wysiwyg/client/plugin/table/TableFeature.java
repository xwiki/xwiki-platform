package com.xpn.xwiki.wysiwyg.client.plugin.table;

import com.google.gwt.user.client.ui.PushButton;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * TableFeature API. 
 * 
 * @version $Id$
 */
public interface TableFeature extends Executable
{
    /**
     * Get feature name.
     * 
     * @return feature name (examples: inserttable, insertrowbefore).
     */
    String getName();
    
    /**
     * Get feature button.
     * 
     * @return feature button.
     */
    PushButton getButton();

    /**
     * Get feature command.
     * 
     * @return feature command.
     */
    Command getCommand();
    
    /**
     * Get table plugin.
     * 
     * @return table plugin.
     */
    TablePlugin getPlugin();
    
    /**
     * Remove feature button from the interface.
     */
    void destroy();
}
