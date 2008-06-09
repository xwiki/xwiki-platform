package com.xpn.xwiki.plugin.diff;

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 1 mai 2007
 * Time: 15:55:04
 * To change this template use File | Settings | File Templates.
 */
public class DiffPluginApi extends Api {
    private DiffPlugin plugin;

    public DiffPluginApi(DiffPlugin plugin, XWikiContext context) {
        super(context);
        setPlugin(plugin);
    }

    public DiffPlugin getPlugin() {
        if (hasProgrammingRights()) {
            return plugin;
        }
        return null;
    }

    public void setPlugin(DiffPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Return a list of Delta objects representing line differences in text1 and text2
     * @param text1 original content
     * @param text2 revised content
     * @return list of Delta objects
     */
    public List getDifferencesAsList(String text1, String text2) throws XWikiException {
        return plugin.getDifferencesAsList(text1, text2);
    }

    /**
     * Return an html blocks representing line diffs between text1 and text2
     * @param text1 original content
     * @param text2 revised content
     * @return list of Delta objects
     */
    public String getDifferencesAsHTML(String text1, String text2) throws XWikiException {
        return plugin.getDifferencesAsHTML(text1, text2);
    }
    
    /**
     * Return an html blocks representing line diffs between text1 and text2
     * @param text1 original content
     * @param text2 revised content
     * @param allDoc view all document or only changes
     * @return list of Delta objects
     */
    public String getDifferencesAsHTML(String text1, String text2, boolean allDoc) throws XWikiException {
        return plugin.getDifferencesAsHTML(text1, text2, allDoc);
    }

    /**
     * Return a list of Delta objects representing word differences in text1 and text2
     * @param text1 original content
     * @param text2 revised content
     * @return list of Delta objects
     */
    public List getWordDifferencesAsList(String text1, String text2) throws XWikiException {
        return plugin.getWordDifferencesAsList(text1, text2);
    }

    /**
     * Return an html blocks representing word diffs between text1 and text2
     * @param text1 original content
     * @param text2 revised content
     * @return list of Delta objects
     */
    public String getWordDifferencesAsHTML(String text1, String text2) throws XWikiException {
        return plugin.getWordDifferencesAsHTML(text1, text2);
    }

}

