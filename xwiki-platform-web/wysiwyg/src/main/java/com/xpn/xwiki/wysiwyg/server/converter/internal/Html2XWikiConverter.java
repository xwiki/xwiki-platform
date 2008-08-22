/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author Christian Gmeiner
 */
package com.xpn.xwiki.wysiwyg.server.converter.internal;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

/**
 * TODO - class and style can coexist - make use of xwikis {style} - <br>
 * tag broken? - rewritte style parser - write test based on html files - make check style happy
 */

/**
 * Represents our magic converter, which is able to convert HTML to Xwiki and Xwiki to HTML.
 * 
 * @version $Id: $
 */
public class Html2XWikiConverter
{
    /**
     * Identifyer for bold in xwiki syntax. Ex: *bold text*
     */
    private static final String BOLD_XWIKI = "*";

    /**
     * Identifyer for italic in xwiki syntax. Ex: ~~italic text~~
     */
    private static final String ITALIC_XWIKI = "~~";

    /**
     * Identifyer for underline in xwiki syntax. Ex: __underline text__
     */
    private static final String UNDERLINE_XWIKI = "__";

    /**
     * Identifyer for a new line.
     */
    private static final String NEW_LINE = "\n";

    /**
     * This constant represents a Node, containing pure text without tags.
     */
    private static final String TEXT_NODE = "#text";

    /**
     * Store all aviable styles (key is tag, object is StyleObject).
     */
    private static final Map<String, StyleObject> STYLES = new HashMap<String, StyleObject>();

    /**
     * Method to convert HTML into Xwikis markup language.
     * 
     * @param input HTML string
     * @return convertert string (Xwiki)
     */
    public static String convertHtml2XWiki(String input)
    {
        StringBuffer buffer = new StringBuffer();
        State state = new State();

        // create a DOM document out of our input html
        Tidy t = new Tidy();

        // dont flood us with warnigns
        t.setQuiet(true);
        t.setShowWarnings(false);

        // parse
        StringInputStream in = new StringInputStream(input);
        Document doc = t.parseDOM(in, null);

        // clear styles
        STYLES.clear();

        // get all childs and do a call
        NodeList childs = doc.getChildNodes();

        for (int i = 0; i < childs.getLength(); i++) {
            node2xwiki(childs.item(i), buffer, state);
        }

        return buffer.toString().trim();
    }

    /**
     * Convert a Node object into xwiki markup.
     * 
     * @param node {@link org.w3c.dom.Node} from our parsed DOM
     * @param buffer which is used to write our converted data in
     * @param state {@link com.xpn.xwiki.wysiwyg.State} from our parent caller. It is very important, if the parent node
     *            influences our current node.
     */
    private static void node2xwiki(Node node, StringBuffer buffer, State state)
    {

        String tag = node.getNodeName();

        // should never happen
        if (tag == null) {
            return;
        }

        if (tag.equals(TEXT_NODE)) {

            // here we need to look in our state
            // object, if the current TEXT_NODE
            // is a real text, or a style to parse.
            if (!state.isStyle()) {
                // should we write the TEXT_NODE?
                if (!state.isExclude()) {
                    buffer.append(code(node.getNodeValue()));
                }
            } else {
                parseStyles(state, node.getNodeValue());
            }
        } else {

            // to make the code cleaner, we split
            // bigger and complxer parts into
            // private methods.

            handleBasics(tag, node, state);
            handleLists(tag, node, state);
            handleTables(tag, node, state);
            handleLinks(tag, node, state);
            handleImages(tag, node, state);
            handleStyles(tag, node, state);
            appendStyles(tag, node, state);

            // write down our insertBefore and pricess
            // our childs, then wirte our insertAfter.

            buffer.append(state.getInsertBefore());

            // get all childs and do a recursive call
            NodeList childs = node.getChildNodes();

            // create new state object for our childs,
            // based on our current state
            State fresh = new State(state);
            for (int i = 0; i < childs.getLength(); i++) {
                node2xwiki(childs.item(i), buffer, fresh);

                // a problem with the current way of working is that,
                // we need to know when we are in a table and when not.
                // the begining of a table is very easy to find, but
                // the end not. So we will set setInTable to false every
                // time, but we will reset it this state. So that every
                // child konws exactly if we are in a table or not
                fresh.setInTable(state.isInTable());
            }

            buffer.append(state.getInsertAfter());

            // it is very safe to reset the state now,
            // because we give the state fresh to every
            // child.
            state.setInsertBefore("");
            state.setInsertAfter("");
            state.setInTable(false);
        }
    }

    /**
     * React on basic tags like strong, em, u,...
     * 
     * @param tag name of the tag - e.g. strong
     * @param node current node {@link org.w3c.dom.Node} which gets processed
     * @param state object {@link com.xpn.xwiki.wysiwyg.State}, which is used for the final converting step.
     */
    private static void handleBasics(String tag, Node node, State state)
    {

        if (tag.equals("strong") || tag.equals("b")) {
            // bold
            state.setInsertBefore(BOLD_XWIKI);
            state.setInsertAfter(BOLD_XWIKI);

        } else if (tag.equals("em") || tag.equals("i")) {
            // italic
            state.setInsertBefore(ITALIC_XWIKI);
            state.setInsertAfter(ITALIC_XWIKI);

        } else if (tag.equals("u")) {
            // underline
            state.setInsertBefore(UNDERLINE_XWIKI);
            state.setInsertAfter(UNDERLINE_XWIKI);

        } else if (tag.equals("br")) {
            state.setInsertAfter("\\\\" + NEW_LINE);

        } else if (tag.equals("p")) {

            if (!state.isInTable()) {
                state.setInsertAfter(NEW_LINE + NEW_LINE);
            }

        } else if (tag.equals("title")) {
            state.setExclude(true);

        } else if (tag.equals("hr")) {
            // horizontal line
            state.setInsertAfter(NEW_LINE + "----" + NEW_LINE);

        }
    }

    /**
     * Check if we need to convert something in connection with lists.
     * 
     * @param tag name of the tag - e.g. strong
     * @param node current node {@link org.w3c.dom.Node} which gets processed
     * @param state object {@link com.xpn.xwiki.wysiwyg.State}, which is used for the final converting step.
     */
    private static void handleLists(String tag, Node node, State state)
    {

        if (tag.equals("ol")) {
            // orderd list
            state.setOrderdList(true);
            state.setUnorderedList(false);

        } else if (tag.equals("ul")) {
            // unorderd list
            state.setOrderdList(false);
            state.setUnorderedList(true);

        } else if (tag.equals("li")) {

            if (state.isOrderdList()) {
                state.setInsertBefore("1. ");
            } else if (state.isUnorderedList()) {
                state.setInsertBefore("* ");
            }

            state.setInsertAfter(NEW_LINE);
        }
    }

    /**
     * Check if we need to convert something in connection with tables.
     * 
     * @param tag name of the tag - e.g. strong
     * @param node current node {@link org.w3c.dom.Node} which gets processed
     * @param state object {@link com.xpn.xwiki.wysiwyg.State}, which is used for the final converting step.
     */
    private static void handleTables(String tag, Node node, State state)
    {

        final String table = "{table}";

        if (tag.equals("table")) {
            state.setInsertBefore(table);
            state.setInsertAfter(NEW_LINE + table);
            state.setInTable(true);

            // at this point we could get the informations
            // what the total number columns are. We need simply
            // go trhough our childs and count the number of
            // TEXT_NODES with "col"

            int count = 0;
            NodeList childs = node.getChildNodes();
            for (int i = 0; i < childs.getLength(); i++) {

                String name = childs.item(i).getNodeName();
                if (name != null && name.equals("col")) {
                    count++;
                }
            }

            // was there "col" child found?
            if (count > 0) {
                state.setTableColumnTotal(count);
            }

        } else if (tag.equals("tr")) {
            // set total number of columns
            state.setTableColumnCount(0);
            state.setTableColumnTotal(node.getChildNodes().getLength());

            state.setInsertBefore(NEW_LINE);

        } else if ((tag.equals("td")) || (tag.equals("th"))) {

            // increase count
            state.increaseTableColumnCount();

            // insert a | only if the number of this
            // column is lesser then the total count of
            // columns
            if (state.getTableColumnCount() < state.getTableColumnTotal()) {
                state.setInsertAfter("|");
            }
        }
    }

    /**
     * Check if we need handle a tags and handle them.
     * 
     * @param tag name of the tag - e.g. strong
     * @param node current node {@link org.w3c.dom.Node} which gets processed
     * @param state object {@link com.xpn.xwiki.wysiwyg.State}, which is used for the final converting step.
     */
    private static void handleLinks(String tag, Node node, State state)
    {

        if (tag.equals("a")) {

            final String beginTag = "[";
            final String endTag = "]";
            final String delimer = ">";
            final String defaultTarget = "_self";

            String url = readAttribute(node, "href");
            String target = readAttribute(node, "target");
            String name = "";

            NodeList childs = node.getChildNodes();

            // search in our content for a string obj
            for (int j = 0; j < childs.getLength(); j++) {
                Node child = childs.item(j);
                String nodeName = child.getNodeName();

                if (nodeName != null && nodeName.equals(TEXT_NODE)) {
                    name = child.getNodeValue();
                }
            }

            // dont show an unvalid link
            if (!url.equals("")) {

                state.setInsertBefore(beginTag);

                if (name.equals("")) {
                    state.setInsertAfter(url);
                } else {

                    // now a trick part follows.
                    // if url and name are equal, we
                    // dont write a delimer and url,
                    // because something like
                    // [http://www.domain.tld>http://www.domain.tld]
                    // is nonsense.
                    // Keep in mind that "name" will be written not
                    // by us, it gets written by obj2string.

                    if (!url.equals(name)) {
                        state.setInsertAfter(delimer + url);
                    }
                }

                // look if we have a taret and it is not
                // equal to defaultTarget
                if (!target.equals("") && !target.equals(defaultTarget)) {
                    state.setInsertAfter(state.getInsertAfter() + delimer + target);
                }

                state.setInsertAfter(state.getInsertAfter() + endTag);
            }
        }
    }

    /**
     * Check if we need handle image tags and handle them.
     * 
     * @param tag name of the tag - e.g. strong
     * @param node current node {@link org.w3c.dom.Node} which gets processed
     * @param state object {@link com.xpn.xwiki.wysiwyg.State}, which is used for the final converting step.
     */
    private static void handleImages(String tag, Node node, State state)
    {

        if (tag.equals("img")) {

            final String beginTag = "{image:";
            final String endTag = "}";
            final String delimer = "|";

            String src = readAttribute(node, "src");
            String width = readAttribute(node, "width");
            String height = readAttribute(node, "height");
            String align = readAttribute(node, "align");
            String halign = readAttribute(node, "halign");

            // look if image tag is valid
            if (!src.equals("")) {

                // remove everything from our src, which is not needed
                int index = src.lastIndexOf("/");

                if (index > 0) {
                    src = src.substring(index + 1);
                }

                // if no width and high is given, simply write
                // the following:
                // {src}
                if (width.equals("") && height.equals("")) {
                    state.setInsertBefore(beginTag + src);
                } else {

                    // if width and height are the same, simply write
                    // the following
                    // {src|height}
                    if (width.equals(height)) {
                        state.setInsertBefore(beginTag + src + delimer + height);
                    } else {

                        // if they are not equal, write the following
                        // {src|height|width}

                        // special case: if width is "", we should only write:
                        // {src|height} and not {src|height|}
                        if (width.equals("")) {
                            state.setInsertBefore(beginTag + src + delimer + height);
                        } else {
                            state.setInsertBefore(beginTag + src + delimer + height + delimer + width);
                        }
                    }
                }

                // here we analyse the aligment stuff
                String aligment = "";

                // look if there is an align
                if (!align.equals("")) {
                    aligment = delimer + align;
                }

                // look if there is a halign
                if (!halign.equals("")) {

                    if (aligment.equals("")) {
                        aligment = delimer;
                    }

                    aligment = aligment + delimer + halign;
                }

                // do we need to add aligment data?
                if (!aligment.equals("")) {
                    state.setInsertBefore(state.getInsertBefore() + aligment);
                }

                // add end tag
                state.setInsertAfter(state.getInsertAfter() + endTag);
            }
        }
    }

    /**
     * Check if we need handle style tags and handle them.
     * 
     * @param tag name of the tag - e.g. strong
     * @param node current node {@link org.w3c.dom.Node} which gets processed
     * @param state object {@link com.xpn.xwiki.wysiwyg.State}, which is used for the final converting step.
     */
    private static void handleStyles(String tag, Node node, State state)
    {

        if (tag.equals("style")) {

            // indicate, that the next pared #text
            // will contain style informations to
            // parse.
            state.setStyle(true);
        }
    }

    /**
     * Convert child od <style></style> and add it to STYLES.
     * 
     * @param state object {@link com.xpn.xwiki.wysiwyg.State}, which is used for the final converting step.
     * @param input String, which contains the styles to parse
     */
    private static void parseStyles(State state, String input)
    {

        int index = 0;

        // reset style indicator
        state.setStyle(false);

        // we are working on a copy
        String in = input.trim();

        // remove <!-- -->
        if (in.startsWith("<!--")) {
            in = in.substring(4, in.length() - 3).trim();
        }

        while (index < in.length() - 1) {

            int start = in.indexOf("{", index);
            int end = in.indexOf("}", index);

            String tag = in.substring(index, start).trim();
            String sub = in.substring(start + 1, end - 1).trim();

            // remove comments from tag
            if (tag.contains("/*")) {
                int s = tag.indexOf("/*");
                int e = tag.lastIndexOf("*/");

                // remove it
                tag = tag.substring(0, s) + tag.substring(e + 2, tag.length()).trim();
            }

            StyleObject obj = new StyleObject();
            parseStyleBlock(obj, sub);

            // only add styleobj if it is dirty
            if (obj.isDirty()) {

                // look if there is a "," in the tag
                String[] tags = tag.split(",");

                if (tags.length > 0) {

                    for (int i = 0; i < tags.length; i++) {
                        STYLES.put(tags[i].trim(), obj);
                    }

                } else {
                    STYLES.put(tag, obj);
                }
            }

            index = end + 1;
        }
    }

    /**
     * This method is used to parse many style definitions, like: option : value; option : value; ...
     * 
     * @param style StyleObject {@link StyleObject} used to fill
     * @param input out input String with all data to parse
     */
    private static void parseStyleBlock(StyleObject style, String input)
    {

        String[] parts = input.split(";");

        for (int i = 0; i < parts.length; i++) {
            parseOneStyle(style, parts[i]);
        }
    }

    /**
     * Little helper ot parse one single line of a style.
     * 
     * @param style StyleObject {@link StyleObject} used to fill
     * @param input our input String with data to parse
     */
    private static void parseOneStyle(StyleObject style, String input)
    {

        // a line of a style has the following syntases:
        // option:value

        String[] parts = input.split(":");

        String option = parts[0].trim();
        String value = parts[1].trim();

        // check for supported options
        if (option.contains("font-weight")) {
            if (value.contains("bold")) {
                style.setBold(true);
            }
        }

        if (option.contains("font-style")) {
            if (value.contains("italic")) {
                style.setItalic(true);
            }
        }

        if (option.contains("font-size")) {
            style.setFontSize(value);
        }

        if (option.contains("font-family")) {
            style.setFontFamily(value);
        }

        if (option.contains("color")) {
            style.setColor(value);
        }

        if (option.contains("width")) {
            style.setWidth(value);
        }

        if (option.contains("hight")) {
            style.setHight(value);
        }
    }

    /**
     * Append any found style.
     * 
     * @param tag name of the tag - e.g. strong
     * @param node current node {@link org.w3c.dom.Node} which gets processed
     * @param state object {@link State}, which is used for the final converting step.
     */
    private static void appendStyles(String tag, Node node, State state)
    {

        String style = readAttribute(node, "style");

        // only try to append a stlye, if we
        // have found styles. There exists only
        // one special case: a style can be also
        // defined in attribute "style"
        if (STYLES.isEmpty() && style.equals("")) {
            return;
        }

        StyleObject obj = null;

        // look if there is a style and if it contains
        // a ":" - is needed because of the style format:
        // option : value
        if (!style.equals("") && style.contains(":")) {

            // create a new object and parse
            // the style
            obj = new StyleObject();
            parseStyleBlock(obj, style);

            // if our style is not dirty,
            // mark it as null, so that
            // we will not try to append this
            // style.
            if (!obj.isDirty()) {
                obj = null;
            }

        } else {

            // read class attribute and try to find a
            // StyleObject
            String styleClass = readAttribute(node, "class");
            obj = findStyle(tag, styleClass);
        }

        // have we found a suitable StyleObject?
        if (obj == null) {
            return;
        }

        // append our StyleObject
        String before = state.getInsertBefore();
        String after = state.getInsertAfter();

        // append styles
        if (obj.isBold()) {
            before = before + BOLD_XWIKI;
            after = BOLD_XWIKI + after;
        }

        if (obj.isItalic()) {
            before = before + ITALIC_XWIKI;
            after = ITALIC_XWIKI + after;
        }

        // try to generate xwiki style
        String styleBlock = generateStyleBlock(obj);

        if (!styleBlock.equals("")) {
            before = before + styleBlock;
            after = "{style}" + after;
        }

        state.setInsertBefore(before);
        state.setInsertAfter(after);
    }

    /**
     * Little helper to read out a wanted attribute.
     * 
     * @param node from {@link org.w3c.dom.Node} which we want an attribute
     * @param attribute the name of the wanted attribute
     * @return the value of the wanted attriubte or ""
     */
    private static String readAttribute(Node node, String attribute)
    {

        String result = "";

        NamedNodeMap attributes = node.getAttributes();

        // go throuh our attributes and look for attribute
        for (int j = 0; j < attributes.getLength(); j++) {
            Node attr = attributes.item(j);

            if (attr.getNodeName().equals(attribute)) {
                result = attr.getNodeValue();
                break;
            }
        }

        return result;
    }

    /**
     * This method searches for a suitable StyleObject.
     * 
     * @param tag String representation of the tag - e.g. div
     * @param styleclass name of the style class to use
     * @return if a style was found a valid instance of StyleObject else null
     */
    private static StyleObject findStyle(String tag, String styleclass)
    {
        StyleObject obj = null;

        // we must look now, if we can find a suitable StyleObject.
        // so we are looking with the following keys
        // * tag.styleClass
        // * .styleClass
        // * tag

        if (!styleclass.equals("")) {

            String stlye = "." + styleclass;
            obj = STYLES.get(tag + stlye);

            // not found... try it with an other name
            if (obj == null) {
                obj = STYLES.get(stlye);
            }
        }

        if (obj == null) {
            obj = STYLES.get(tag);
        }

        return obj;
    }

    private static String generateStyleBlock(StyleObject obj)
    {

        StringBuffer buffer = new StringBuffer();

        // xwiki has support for more advanced styles
        // it useses for this {style}
        if (obj.isStyleTag()) {
            buffer.append("{style:");

            if (obj.getFontSize() != null) {
                buffer.append("font-size=");
                buffer.append(obj.getFontSize());
                buffer.append("|");
            }

            if (obj.getColor() != null) {
                buffer.append("color=");
                buffer.append(obj.getColor());
                buffer.append("|");
            }

            if (obj.getFontFamily() != null) {
                buffer.append("font-family=");
                buffer.append(obj.getFontFamily());
                buffer.append("|");
            }

            if (obj.getWidth() != null) {
                buffer.append("width=");
                buffer.append(obj.getWidth());
                buffer.append("|");
            }

            if (obj.getHight() != null) {
                buffer.append("height=");
                buffer.append(obj.getHight());
                buffer.append("|");
            }

            // remove last |
            if (buffer.length() > 7) {
                buffer.delete(buffer.length() - 1, buffer.length());
            }

            buffer.append("}");
        }

        return buffer.toString();
    }

    /**
     * @param input String to code for xwiki syntax
     * @return xwiki compatible String
     */
    private static String code(String input)
    {

        String result = input;

        // convert a \ to \\\
        result = result.replace("\\", "\\\\\\");

        return result;
    }
}
