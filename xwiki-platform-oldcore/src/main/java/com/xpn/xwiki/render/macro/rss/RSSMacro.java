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
 *
 */


package com.xpn.xwiki.render.macro.rss;

import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.TextInput;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndImage;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.apache.commons.beanutils.BeanUtils;
import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;
import org.radeox.util.Encoder;
import org.radeox.util.logging.Logger;

import java.io.Writer;
import java.net.MalformedURLException;
import java.util.List;

/**
 * A Radeox MacroFilter for rendering an RSS feed.
 * @author Joe Germuska
 * @version 0.2d
 */

public class RSSMacro extends BaseLocaleMacro {

    private static final String NAME = "rss";

    private static final String DESCRIPTION = "Use to aggregate RSS feeds";

    private static final String[] PARAM_DESCRIPTION = new String[] {
        "feed: url of an RSS feed",
        "?img: if 'true' and if feed has an image, image will be included",
        "?align: if an image will be included, use this alignment",
        "?css: if 'true', elements will be created with CSS styles; otherwise, static formatting methods will be used",
        "?count: an integer, the maximum number of feed items to display",
        "?full: if 'true', descriptions for each item will be included.  Otherwise, just titles.",
        "?search: if 'true' and if feed has a search field, field will be included"
    };

    private static final String[] PARAM_NAMES = new String[] {
        "img", "align", "css", "count", "full", "search"
    };

    private static final char NEWLINE = '\n';


    public String getLocaleKey() {
        return "macro.rss";
    }

    public String getName() {
        return NAME;
    }

    /**
     * Process the macro.
     * @param writer the output writer
     * @param parameter the input parameters of the macro.
     * @throws java.lang.IllegalArgumentException
     * @throws java.io.IOException from calls to <code>writer.write()</code>
     * TODO Make commons-digester understand more different RSS feeds, or switch
     * to a better RSS library.
     */
    public void execute(Writer writer, MacroParameter parameter) throws java.lang.IllegalArgumentException, java.io.IOException {

        RSSMacroParameters paramObj = processParameters(parameter);

        java.net.URL feedURL = paramObj.getFeedURL();

        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = null;
        try {
            feed = input.build(new XmlReader(feedURL));
        }
        catch (Exception ex) {
            throw new java.io.IOException("Error processing " + feedURL + ": " + ex.getMessage());
        }
        if (feed == null) throw new IllegalArgumentException("No feed found at " + feedURL);
        if (paramObj.isCss())
        {
            writer.write(NEWLINE);
            writer.write("<div class='rssfeed'>");
        }
        renderImage(feed, writer, paramObj);
        renderTitle(feed, writer, paramObj);
        renderEntries(feed, writer, paramObj);
        renderSearch(feed, writer, paramObj);
        writer.write(NEWLINE);
        writer.write(NEWLINE);

        if (paramObj.isCss())
        {
            writer.write("</div>");
        }
    }

    /**
     * Render as many of the given <code>Channel</code>'s items as needed,
     * according to the value of the optional <code>count</code> parameter
     * and the number of items in the feed.
     * @param writer the output writer
     * @param paramObj our parameter helper object
     * @throws java.io.IOException
     */
    private void renderEntries(SyndFeed feed, Writer writer, RSSMacroParameters paramObj) throws java.io.IOException
    {
        List listEntries = feed.getEntries();

        int max = paramObj.evalCount(listEntries.size());

        for (int i = 0; i < max; i++) {
          renderEntry((SyndEntry)listEntries.get(i), writer, paramObj);
        }
    }


    /**
     * Render the given RSS <code>Item</code> according to whether or not the
     * parameters call for CSS processing.
     * @param writer the output writer
     * @param paramObj our parameter helper object
     * @throws java.io.IOException from calls to <code>writer.write()</code>
     */
    private void renderEntry(SyndEntry entry, Writer writer, RSSMacroParameters paramObj) throws java.io.IOException
    {
        StringBuffer buf = new StringBuffer();
        if (paramObj.isCss()){
            renderEntryCSS(entry, buf, paramObj);
        } else {
            renderEntryDefault(entry, buf, paramObj);
        }
        writer.write(buf.toString());
    }

    /**
     * Render the given <code>Item</code> using Radeox macros.
     * <ul>
     * <li>The item title is wrapped in <code>__bold__</code></li>
     * <li>The link is rendered using the <code>{link}</code> macro.</li>
     * <li>If present, the item description is wrapped in <code>{quote}</code> macro tags.</li>
     * </ul>
     * @param buf the StringBuffer we're using to prepare the output
     * @param paramObj our parameter helper object
     */
    private void renderEntryDefault(SyndEntry entry, StringBuffer buf, RSSMacroParameters paramObj) {
        buf.append(NEWLINE).append("* ");
        if (entry.getLink() != null) { buf.append("{link:"); }
        buf.append(entry.getTitle());
        if (entry.getLink() != null) {
            buf.append("|")
               .append(entry.getLink())
               .append("}");
        }

        if (paramObj.isFull() && entry.getDescription() != null)
        {
            buf.append(NEWLINE).append("{quote}")
               .append(NEWLINE).append(entry.getDescription().getValue())
               .append(NEWLINE).append("{quote}");
        }
    }

    /**
     * Render the given <code>Item</code> using <code>&lt;div&gt;</code> tags
     * with CSS class attributes.
     * <ul>
     * <li>The whole item is wrapped in <code>&lt;div class='rss.item'&gt;</li>
     * <li>The item title is wrapped in <code>&lt;div class='rss.item.title'&gt;</li>
     * <li>The item title link is wrapped in <code>&lt;a class='rss.item.title'&gt;</code></li>
     * <li>If present, the item description is wrapped in <code>&lt;div class='rss.item.description'&gt;</li>
     * </ul>
     * @param buf the StringBuffer we're using to prepare the output
     * @param paramObj our parameter helper object
     * TODO Figure out how to stop Radeox from filtering the URLs
     */
    private void renderEntryCSS(SyndEntry entry, StringBuffer buf, RSSMacroParameters paramObj) {
        buf.append(NEWLINE).append("<div class='rssitem'>");
        buf.append(NEWLINE).append("<div class='rssitemtitle'>").append(NEWLINE);
        if (entry.getLink() != null)
        {
            buf.append("<a class='rssitemtitle' href=\"")
               .append(Encoder.escape(entry.getLink()))
               .append("\">");
        }
        buf.append(entry.getTitle());
        if (entry.getLink() != null) { buf.append("</a>"); }
        buf.append(NEWLINE).append("</div>"); // close rss.item.title
        if (paramObj.isFull() && entry.getDescription() != null)
        {
            buf.append(NEWLINE).append("<div class='rssitemdescription'>")
               .append(NEWLINE).append(entry.getDescription().getValue())
               .append(NEWLINE).append("</div>"); // close rss.item.description
        }
        buf.append(NEWLINE).append("</div>"); // close rss.item
    }

    /**
     * Render the 'title' of the given RSS Channel to the <code>Writer</code>.
     * @param feed the RSS Channel we retrieved via the Feed URL
     * @param writer the output writer
     * @param paramObj our parameter helper object
     * @throws java.io.IOException from calls to <code>writer.write()</code>
     */
    private void renderTitle(SyndFeed feed, Writer writer, RSSMacroParameters paramObj) throws java.io.IOException
    {
        StringBuffer buf = new StringBuffer();
        if (paramObj.isCss())
        {
            renderTitleCSS(feed, buf);
        } else {
            renderTitleDefault(feed, buf);
        }
        writer.write(buf.toString());
    }

    /**
     * Render the title from the given Channel to the given StringBuffer,
     * using standard Radeox filtering tags.
     * @param feed the RSS Channel we retrieved via the Feed URL
     * @param buf the StringBuffer we're using to prepare the output
     */
    private void renderTitleDefault(SyndFeed feed, StringBuffer buf) {
        buf.append("__");
        if (feed.getLink() != null) buf.append("{link:");
        buf.append(feed.getTitle());
        if (feed.getLink() != null)
        {
            buf.append("|")
               .append(feed.getLink())
               .append("}");
        }
        buf.append("__\n\n");
    }

    /**
     * Render the title from the given Channel to the given StringBuffer,
     * using CSS styled 'div' tags.  In this case, the title will be enclosed
     * in a &lt;div&gt; with the class <code>rss.channel.title</code>.  If
     * the channel includes a link, the title will be rendered as a link to
     * that URL, and the &lt;a&gt; tag will also be of class
     * <code>rss.channel.title</code>
     * @param feed the RSS Channel we retrieved via the Feed URL
     * @param buf the StringBuffer we're using to prepare the output
     * TODO Figure out how to stop Radeox from filtering the URLs
     */
    private void renderTitleCSS(SyndFeed feed, StringBuffer buf) {
        buf.append(NEWLINE).append("<div class='rsschanneltitle'>");
        if (feed.getLink() != null)
        {
            buf.append("<a class='rsschanneltitle' href=\"")
                    .append(Encoder.escape(feed.getLink()))
                    .append("\">");
        }
        buf.append(feed.getTitle());
        if (feed.getLink() != null) buf.append("</a>");
        buf.append(NEWLINE).append("</div>");
    }

    /**
     * If a parameter was passed with the name "img" and the literal value "true",
     * render the image from the channel (if it has one.)  This requires the use
     * of named parameters.
     * @param feed the RSS Channel we retrieved via the Feed URL
     * @param writer the output writer
     * @param paramObj our parameter helper object
     * @throws java.io.IOException from calls to <code>writer.write()</code>
     */
    private void renderImage(SyndFeed feed, Writer writer, RSSMacroParameters paramObj) throws java.io.IOException
    {
        if (feed.getImage() == null) return;
        if (!(paramObj.isImg())) return;

        SyndImage rssImage = feed.getImage();
        StringBuffer buf = new StringBuffer(NEWLINE + "{image:");
        buf.append("img=").append(rssImage.getUrl());
        buf.append("|link=").append(feed.getLink());
        buf.append("|align=").append(paramObj.getAlign());
        if (rssImage.getDescription() != null)
        {
            buf.append("|alt=").append(rssImage.getDescription());
        }
        buf.append("}");
        Logger.debug("*** RSS image: " + buf);
        writer.write(buf.toString());
    }

    /**
     * CSS styles are used because there is no way
     * to render this using 'default' because org.snipsnap.render.macro.FieldMacro
     * only permits form submission to other snips.  Body of this method
     * essentially adapted from that implementation.
     * Entire block will be wrapped in a div, class 'rss.textinput'.
     * Description will be in a div, class 'rss.textinput.description'.
     * &lt;form&gt; element will be class 'rss.textinput.form'.
     * Field will be an HTML &lt;input&gt; (type text) tag, class 'rss.textinput.field'.
     * Submit button will be an HTML &lt;input&gt; (type submit), class
     * 'rss.textinput.submit'.
     * @param feed the RSS Channel we retrieved via the Feed URL
     * @param writer the output writer
     * @param paramObj our parameter helper object
     * @throws java.io.IOException from calls to <code>writer.write()</code>
     */
    private void renderSearch(SyndFeed feed, Writer writer, RSSMacroParameters paramObj) throws java.io.IOException
    {
        if (!(paramObj.isSearch())) return;
        WireFeed wireFeed = feed.createWireFeed();
        if (!(wireFeed instanceof Channel)) return;
        TextInput textInput = ((Channel)wireFeed).getTextInput();
        if (textInput == null) return;
        writer.write(NEWLINE);
        writer.write("\\\\");
        writer.write(NEWLINE);
        writer.write("<div class='rsstextinput'>");
        writer.write(NEWLINE);
        writer.write("<div class='rsstextinputdescription'>");
        writer.write(textInput.getDescription());
        writer.write("</div>"); // end rss.textinput.description
        writer.write(NEWLINE);
        writer.write("<form class='rsstextinputform' action=\"");
        writer.write(Encoder.escape(textInput.getLink()));
        writer.write("\" method=\"get\">");
        writer.write(NEWLINE);
        writer.write("<input class='rsstextinputfield' size=\"18\" name=\"");
        writer.write(textInput.getName());
        writer.write("\"");
        writer.write("/>");
        writer.write(NEWLINE);
        writer.write(" <input class='rsstextinputsubmit' type=\"submit\" name=\"submit\" value=\"");
        writer.write(textInput.getTitle());
        writer.write("\"/>");
        writer.write("</form>");
        writer.write(NEWLINE);
        writer.write("</div>"); // end rss.textinput
    }

    /**
     * @return the MacroFilter's parameter descriptions:
     * <ul>
     * <li>feed: url of an RSS feed</li>
     * <li>?img: if 'true' and if feed has an image, image will be included</li>
     * <li>?align: if an image will be included, use this alignment</li>
     * <li>?css: if 'true', elements will be created with CSS styles; otherwise, static formatting methods will be used</li>
     * <li>?count: an integer, the maximum number of feed items to display</li>
     * <li>?full: if 'true', descriptions for each item will be included.  Otherwise, just titles.</li>
     * <li>?search: if 'true' and if feed has a search field, field will be included</li>
     * </ul>
     * Note that all parameters must be passed using names.
     */
    public String[] getParamDescription() {
        return PARAM_DESCRIPTION;
    }


    /**
     * @return the MacroFilter's description: 'Use to aggregate RSS feeds'.
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Transform the input parameters into <code>RSSMacroParameters</code> object.
     * @param parameter the parameters as prepared by the Radeox system.
     * @throws java.lang.IllegalArgumentException if the 'feed' named parameter
     * is missing, or is a malformed URL, or if any of the other parameter values
     * are not of the correct types.  Note that unknown parameters will simply
     * be ignored, and all parameters must be passed using names.
     * @return a parameters helper object
     * @see RSSMacroParameters
     */
    private RSSMacroParameters processParameters(MacroParameter parameter) throws java.lang.IllegalArgumentException
    {
        java.util.HashMap paramMap = new java.util.HashMap();
        RSSMacroParameters paramObj = new RSSMacroParameters();
        String feedURLString = parameter.get("feed");
        if (feedURLString == null)
        {
            throw new IllegalArgumentException("Requires at least one named parameter,'feed', a URL of an RSS feed.");
        }
        try {
            paramObj.setFeed(feedURLString);
        }
        catch (MalformedURLException ex) {
            Logger.warn("Invalid feed URL: " + feedURLString);
            throw new IllegalArgumentException("Invalid feed URL: " + feedURLString);
        }
        for (int i = 0; i < PARAM_NAMES.length; i++) {
            String paramName = parameter.get(PARAM_NAMES[i]);
            if (paramName != null)
            {
                paramMap.put(PARAM_NAMES[i],paramName);
            }
        }
        try {
            BeanUtils.populate(paramObj, paramMap);
        }
        catch (NoClassDefFoundError e) {
            throw new IllegalStateException("Some libraries are not installed: " + e.getMessage());
        }
        catch (Exception ex) {
            throw new IllegalArgumentException("Error processing arguments: " + ex.getMessage());
        }

        return paramObj;
    }


}

