/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.plugin.diff;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.suigeneris.jrcs.diff.Diff;
import org.suigeneris.jrcs.diff.Revision;
import org.suigeneris.jrcs.diff.delta.Chunk;
import org.suigeneris.jrcs.diff.delta.Delta;
import org.suigeneris.jrcs.util.ToString;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * @version $Id$
 * @deprecated since 4.1 use diff service
 */
@Deprecated
public class DiffPlugin extends XWikiDefaultPlugin
{
    /**
     * @param name the plugin name, usually ignored, since plugins have a fixed name
     * @param className the name of this class, ignored
     * @param context the current request context
     */
    public DiffPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    @Override
    public String getName()
    {
        return "diff";
    }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new DiffPluginApi((DiffPlugin) plugin, context);
    }

    /**
     * Return a list of Delta objects representing line differences in text1 and text2
     *
     * @param text1 original content
     * @param text2 revised content
     * @return list of Delta objects
     */
    public List getDifferencesAsList(String text1, String text2) throws XWikiException
    {
        try {
            if (text1 == null) {
                text1 = "";
            }
            if (text2 == null) {
                text2 = "";
            }
            return getDeltas(Diff.diff(ToString.stringToArray(text1), ToString.stringToArray(text2)));
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_CONTENT_ERROR,
                "Diff of content generated an exception", e);
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

    protected String escape(String text)
    {
        return XMLUtils.escape(text);
    }

    /**
     * Return a list of Delta objects representing word differences in text1 and text2
     *
     * @param text1 original content
     * @param text2 revised content
     * @return list of Delta objects
     */
    public List getWordDifferencesAsList(String text1, String text2) throws XWikiException
    {
        try {
            text1 = text1.replaceAll(" ", "\n");
            text2 = text2.replaceAll(" ", "\n");
            return getDeltas(Diff.diff(ToString.stringToArray(text1), ToString.stringToArray(text2)));
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_CONTENT_ERROR,
                "Diff of content generated an exception", e);
        }
    }

    /**
     * Return an html blocks representing word diffs between text1 and text2
     *
     * @param text1 original content
     * @param text2 revised content
     * @return list of Delta objects
     */
    public String getWordDifferencesAsHTML(String text1, String text2) throws XWikiException
    {
        text1 = "~~PLACEHOLDER~~" + text1 + "~~PLACEHOLDER~~";
        text2 = "~~PLACEHOLDER~~" + text2 + "~~PLACEHOLDER~~";

        StringBuilder html = new StringBuilder("<div class=\"diffmodifiedline\">");
        List list = getWordDifferencesAsList(text1, text2);
        String[] words = StringUtils.splitPreserveAllTokens(text1, ' ');
        int cursor = 0;
        boolean addSpace = false;

        for (int i = 0; i < list.size(); i++) {
            if (addSpace) {
                html.append(" ");
                addSpace = false;
            }

            Delta delta = (Delta) list.get(i);
            int position = delta.getOriginal().anchor();
            // First we fill in all text that has not been changed
            while (cursor < position) {
                html.append(escape(words[cursor]));
                html.append(" ");
                cursor++;
            }
            // Then we fill in what has been removed
            Chunk orig = delta.getOriginal();
            if (orig.size() > 0) {
                html.append("<span class=\"diffremoveword\">");
                List chunks = orig.chunk();
                for (int j = 0; j < chunks.size(); j++) {
                    if (j > 0) {
                        html.append(" ");
                    }
                    html.append(escape((String) chunks.get(j)));
                    cursor++;
                }
                html.append("</span>");
                addSpace = true;
            }

            // Then we fill in what has been added
            Chunk rev = delta.getRevised();
            if (rev.size() > 0) {
                html.append("<span class=\"diffaddword\">");
                List chunks = rev.chunk();
                for (int j = 0; j < chunks.size(); j++) {
                    if (j > 0) {
                        html.append(" ");
                    }
                    html.append(escape((String) chunks.get(j)));
                }
                html.append("</span>");
                addSpace = true;
            }
        }

        // First we fill in all text that has not been changed
        while (cursor < words.length) {
            if (addSpace) {
                html.append(" ");
            }
            html.append(escape(words[cursor]));
            addSpace = true;
            cursor++;
        }

        html.append("</div>");
        return html.toString().replaceAll("~~PLACEHOLDER~~", "");
    }

    /**
     * Return an html blocks representing line diffs between text1 and text2
     *
     * @param text1 original content
     * @param text2 revised content
     * @return list of Delta objects
     */
    public String getDifferencesAsHTML(String text1, String text2) throws XWikiException
    {
        return getDifferencesAsHTML(text1, text2, true);
    }

    /**
     * Return an html blocks representing line diffs between text1 and text2
     *
     * @param text1 original content
     * @param text2 revised content
     * @param allDoc show all document
     * @return list of Delta objects
     */
    public String getDifferencesAsHTML(String text1, String text2, boolean allDoc) throws XWikiException
    {
        StringBuilder html = new StringBuilder("<div class=\"diff\">");
        if (text1 == null) {
            text1 = "";
        }
        if (text2 == null) {
            text2 = "";
        }
        List list = getDifferencesAsList(text1, text2);
        String[] lines = ToString.stringToArray(text1);
        int cursor = 0;
        boolean addBR = false;

        for (int i = 0; i < list.size(); i++) {
            if (addBR) {
                addBR = false;
            }

            Delta delta = (Delta) list.get(i);
            int position = delta.getOriginal().anchor();
            // First we fill in all text that has not been changed
            while (cursor < position) {
                if (allDoc) {
                    html.append("<div class=\"diffunmodifiedline\">");
                    String text = escape(lines[cursor]);
                    if (text.equals("")) {
                        text = "&nbsp;";
                    }
                    html.append(text);
                    html.append("</div>");
                }
                cursor++;
            }

            // Then we fill in what has been removed
            Chunk orig = delta.getOriginal();
            Chunk rev = delta.getRevised();
            int j1 = 0;

            if (orig.size() > 0) {
                List chunks = orig.chunk();
                int j2 = 0;
                for (int j = 0; j < chunks.size(); j++) {
                    String origline = (String) chunks.get(j);
                    if (origline.equals("")) {
                        cursor++;
                        continue;
                    }
                    // if (j>0)
                    // html.append("<br/>");
                    List revchunks = rev.chunk();
                    String revline = "";
                    while ("".equals(revline)) {
                        revline = (j2 >= revchunks.size()) ? null : (String) revchunks.get(j2);
                        j2++;
                        j1++;
                    }
                    if (revline != null) {
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
            if (rev.size() > 0) {
                List chunks = rev.chunk();
                for (int j = j1; j < chunks.size(); j++) {
                    // if (j>0)
                    // html.append("<br/>");
                    html.append("<div class=\"diffmodifiedline\">");
                    html.append("<span class=\"diffaddword\">");
                    html.append(escape((String) chunks.get(j)));
                    html.append("</span></div>");
                }
                addBR = true;
            }
        }

        // First we fill in all text that has not been changed
        if (allDoc) {
            while (cursor < lines.length) {
                html.append("<div class=\"diffunmodifiedline\">");
                String text = escape(lines[cursor]);
                if (text.equals("")) {
                    text = "&nbsp;";
                }
                html.append(text);
                html.append("</div>");
                cursor++;
            }
        }
        html.append("</div>");
        return html.toString();
    }

}
