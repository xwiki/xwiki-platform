package com.xpn.xwiki.plugin.diff;

import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;
import org.suigeneris.jrcs.diff.Revision;
import org.suigeneris.jrcs.diff.Diff;
import org.suigeneris.jrcs.diff.delta.Delta;
import org.suigeneris.jrcs.diff.delta.Chunk;
import org.suigeneris.jrcs.util.ToString;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.api.Api;

import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 1 mai 2007
 * Time: 15:53:00
 * To change this template use File | Settings | File Templates.
 */
public class DiffPlugin extends XWikiDefaultPlugin {
    /**
     * Log4J logger object to log messages in this class.
     */
    private static final Logger LOG = Logger.getLogger(DiffPlugin.class);

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public DiffPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getName()
     */
    public String getName()
    {
        return "diff";
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getPluginApi
     */
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new DiffPluginApi((DiffPlugin) plugin, context);
    }

    /**
     * Return a list of Delta objects representing line differences in text1 and text2
     * @param text1 original content
     * @param text2 revised content
     * @return list of Delta objects
     */
    public List getDifferencesAsList(String text1, String text2) throws XWikiException {
        try {
            if (text1==null)
             text1 = "";
            if (text2==null)
             text2 = "";
            return getDeltas(Diff.diff(ToString.stringToArray(text1), ToString.stringToArray(text2)));
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_CONTENT_ERROR, "Diff of content generated an exception", e);
        }
    }

    protected List getDeltas(Revision rev)
    {
        ArrayList list = new ArrayList();
        for (int i = 0; i < rev.size(); i++) {
            list.add(rev.getDelta(i));
        }
        return list;
    }


    protected String escape(String text) {
        return XWiki.getXMLEncoded(text);
    }

    /**
     * Return a list of Delta objects representing word differences in text1 and text2
     * @param text1 original content
     * @param text2 revised content
     * @return list of Delta objects
     */
    public List getWordDifferencesAsList(String text1, String text2) throws XWikiException {
        try {
            text1 = text1.replaceAll(" ", "\n");
            text2 = text2.replaceAll(" ", "\n");
            return getDeltas(Diff.diff(ToString.stringToArray(text1), ToString.stringToArray(text2)));
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_CONTENT_ERROR, "Diff of content generated an exception", e);
        }
    }



    /**
     * Return an html blocks representing word diffs between text1 and text2
     * @param text1 original content
     * @param text2 revised content
     * @return list of Delta objects
     */
    public String getWordDifferencesAsHTML(String text1, String text2) throws XWikiException {
        text1 = "~~PLACEHOLDER~~" + text1 + "~~PLACEHOLDER~~";
        text2 = "~~PLACEHOLDER~~" + text2 + "~~PLACEHOLDER~~";

        StringBuffer html = new StringBuffer("<div class=\"diffmodifiedline\">");
        List list = getWordDifferencesAsList(text1, text2);
        String[] words = StringUtils.splitPreserveAllTokens(text1, ' ');
        int cursor = 0;
        boolean addSpace = false;

        for (int i=0;i<list.size();i++) {
            if (addSpace) {
             html.append(" ");
             addSpace = false;
            }

            Delta delta = (Delta)list.get(i);
            int position = delta.getOriginal().anchor();
            // First we fill in all text that has not been changed
            while (cursor<position) {
                html.append(escape(words[cursor]));
                html.append(" ");
                cursor++;
            }
            // Then we fill in what has been removed
            Chunk orig = delta.getOriginal();
            if (orig.size()>0) {
                html.append("<span class=\"diffremoveword\">");
                List chunks = orig.chunk();
                for (int j=0;j<chunks.size();j++) {
                    if (j>0)
                        html.append(" ");
                    html.append(escape((String) chunks.get(j)));
                    cursor++;
                }
                html.append("</span>");
                addSpace = true;
            }

            // Then we fill in what has been added
            Chunk rev = delta.getRevised();
            if (rev.size()>0) {
                html.append("<span class=\"diffaddword\">");
                List chunks = rev.chunk();
                for (int j=0;j<chunks.size();j++) {
                    if (j>0)
                        html.append(" ");
                    html.append(escape((String)chunks.get(j)));
                }
                html.append("</span>");
                addSpace = true;
            }
        }

        // First we fill in all text that has not been changed
        while (cursor<words.length) {
            if (addSpace) 
             html.append(" ");
            html.append(escape(words[cursor]));
            addSpace = true;
            cursor++;
        }

        html.append("</div>");
        return html.toString().replaceAll("~~PLACEHOLDER~~","");
    }

    /**
     * Return an html blocks representing line diffs between text1 and text2
     * @param text1 original content
     * @param text2 revised content
     * @return list of Delta objects
     */
    public String getDifferencesAsHTML(String text1, String text2) throws XWikiException {
        return getDifferencesAsHTML(text1, text2, true);
    }
    /**
     * Return an html blocks representing line diffs between text1 and text2
     * @param text1 original content
     * @param text2 revised content
     * @param allDoc show all document
     * @return list of Delta objects
     */
    public String getDifferencesAsHTML(String text1, String text2, boolean allDoc) throws XWikiException {
        StringBuffer html = new StringBuffer("<div class=\"diff\">");
        if (text1==null)
         text1 = "";
        if (text2==null)
         text2 = "";
        List list = getDifferencesAsList(text1, text2);
        String[] lines = ToString.stringToArray(text1);
        int cursor = 0;
        boolean addBR = false;

        for (int i=0;i<list.size();i++) {
            if (addBR) {
             addBR = false;
            }

            Delta delta = (Delta)list.get(i);
            int position = delta.getOriginal().anchor();
            // First we fill in all text that has not been changed
            while (cursor<position) {
                if (allDoc) {
                    html.append("<div class=\"diffunmodifiedline\">");
                    String text = escape(lines[cursor]);
                    if (text.equals(""))
                        text = "&nbsp;";
                    html.append(text);
                    html.append("</div>");
                }
                cursor++;
            }

            // Then we fill in what has been removed
            Chunk orig = delta.getOriginal();
            Chunk rev = delta.getRevised();
            int j1 = 0;

            if (orig.size()>0) {
                List chunks = orig.chunk();
                int j2 = 0;
                for (int j=0;j<chunks.size();j++) {
                    String origline = (String) chunks.get(j);
                    if (origline.equals("")) {
                        cursor++;
                        continue;
                    }
                    // if (j>0)
                    //    html.append("<br/>");
                    List revchunks = rev.chunk();
                    String revline = "";
                    while ("".equals(revline)) {
                      revline = (j2 >= revchunks.size()) ? null : (String) revchunks.get(j2);
                      j2++;
                      j1++;
                    }
                    if (revline!=null) {
                        html.append(getWordDifferencesAsHTML(origline, revline));
                    } else {
                        html.append("<div class=\"diffmodifiedline\">");
                        html.append("<span class=\"diffremoveword\">");
                        html.append(escape(origline));
                        html.append("</span></div>");
                    }
                    addBR = true;
                    cursor++;
                }
            }

            // Then we fill in what has been added
            if (rev.size()>0) {
                List chunks = rev.chunk();
                for (int j=j1;j<chunks.size();j++) {
                    // if (j>0)
                    //    html.append("<br/>");
                    html.append("<div class=\"diffmodifiedline\">");
                    html.append("<span class=\"diffaddword\">");
                    html.append(escape((String)chunks.get(j)));
                    html.append("</span></div>");
                }
                addBR = true;
            }
        }

        // First we fill in all text that has not been changed
        if (allDoc) {
            while (cursor<lines.length) {
                html.append("<div class=\"diffunmodifiedline\">");
                String text = escape(lines[cursor]);
                if (text.equals(""))
                    text = "&nbsp;";
                html.append(text);
                html.append("</div>");
                cursor++;
            }
        }
        html.append("</div>");
        return html.toString();
    }

}
