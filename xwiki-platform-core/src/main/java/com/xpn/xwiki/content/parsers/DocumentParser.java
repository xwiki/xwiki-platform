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
package com.xpn.xwiki.content.parsers;

import com.xpn.xwiki.content.Link;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;

/**
 * Parse document source content as typed by the user.
 *
 * <p>Note: This is a very basic parser which currently only parses wiki links. It should probably
 * not be developed further. Instead we should migrate to a proper parser such as
 * <a href="http://wikimodel.sourceforge.net/">WikiModel</a>. In any case parsing should be done
 * with a proper parser, such as ANTLR, JavaCC, etc.</p> 
 *
 * @version $Id$
 */
public class DocumentParser implements ContentParser
{
    /**
     * The regex pattern to recognize a wiki link.  
     */
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.*?)\\]");

    /**
     * Parse the links contained into the passed content represent the raw content from a document
     * (as typed by the user).
     * 
     * @param contentToParse the raw document content to parse.
     * @return a list of {@link Link} objects containing the parsed links and a list of invalid
     *         link contents found, returned as a {@link ParsingResultCollection}. This allows
     *         users of this method to decide what they want to do with invalid links (like
     *         report them to the user, fix them, generate an error, etc).
     */
    public ParsingResultCollection parseLinks(String contentToParse)
    {
        ParsingResultCollection results = new ParsingResultCollection();
        LinkParser linkParser = new LinkParser();
        Matcher matcher = LINK_PATTERN.matcher(contentToParse);

        while (matcher.find()) {
            parseLink(linkParser, matcher.group(1), results);
        }

        return results;
    }

    /**
     * Parse the links contained into the passed content represent the raw content from a document
     * (as typed by the user) and replace links that matches the passed linkToLookFor Link with the
     * specified newLink link. The comparison between found links and the link to look for is done
     * by the ReplaceLinkHandler passed as parameter.
     *
     * @param contentToParse the raw document content to parse.
     * @param linkToLookFor the link to look for that will be replaced by the new link
     * @param newLink the new link
     * @param linkHandler the handler to use for comparing the links and for deciding what the
     *        replaced link will look like. For example two links may be pointing to the same
     *        document but one link may have a different alias or target. The handler decides
     *        what to do in these cases.
     * @param currentSpace the space to use for normalizing links. This is used for links that have
     *        no space defined.
     * @return a list of {@link Link} objects containing the parsed links, a list of invalid
     *         link contents found and a list of replaced links, returned as a
     *         {@link ReplacementResultCollection}. This allows users of this method to decide what
     *         they want to do with invalid links (like report them to the user, fix them, generate
     *         an error, etc).
     */
    public ReplacementResultCollection parseLinksAndReplace(String contentToParse,
        Link linkToLookFor, Link newLink, ReplaceLinkHandler linkHandler, String currentSpace)
    {
        ReplacementResultCollection results = new ReplacementResultCollection();
        LinkParser linkParser = new LinkParser();
        Matcher matcher = LINK_PATTERN.matcher(contentToParse);
        StringBuffer modifiedContent = new StringBuffer();
        
        Link normalizedLinkToLookFor = linkToLookFor.getNormalizedLink(currentSpace);
        Link nomalizedNewLink = newLink.getNormalizedLink(currentSpace);

        while (matcher.find()) {
            Link foundLink = parseLink(linkParser, matcher.group(1), results);

            // Verify if the link found matches the link to look for
            if (foundLink != null) {
                Link normalizedFoundLink = foundLink.getNormalizedLink(currentSpace);

                if (linkHandler.compare(normalizedLinkToLookFor, normalizedFoundLink)) {

                    // Compute the replacement string to use. This string must have "$" and
                    // "\" symbols escaped as otherwise "$" will be considered as a regex group
                    // replacement and "\" as a regex escape.
                    // Note: We need to replace the "\" before the "$" as the "$" replacement
                    // introduces other backslashes which would themselves be espaced...
                    String replacementText = "[" + linkHandler.getReplacementLink(
                        nomalizedNewLink, normalizedFoundLink).toString() + "]";
                    replacementText = StringUtils.replace(replacementText, "\\", "\\\\");
                    replacementText = StringUtils.replace(replacementText, "$", "\\$");

                    matcher.appendReplacement(modifiedContent, replacementText);
                    results.addReplacedElement(normalizedFoundLink);
                }
            }
        }
        matcher.appendTail(modifiedContent);
        results.setModifiedContent(modifiedContent.toString());

        return results;
    }

    /**
     * Helper method to parse a link and add the result of the parsing to the resulting collections
     * of objects to be returned to the user.
     *
     * @param parser the link parser to use for parsing the link
     * @param linkContent the content to parse
     * @param results the resulting collections of objects (valid elements, invalid elements) to
     *        pass back to the user. See
              {@link com.xpn.xwiki.content.parsers.ParsingResultCollection} for more details
     * @return the parsed link or null if an error happened during the parsing (invalid link)
     */
    private Link parseLink(LinkParser parser, String linkContent, ParsingResultCollection results)
    {
        Link link = null;
        try {
            link = parser.parse(linkContent);
            results.addValidElement(link);
        } catch (ContentParserException e) {
            // Failed to parse the found link. Add it as an invalid element in the result
            results.addInvalidElementId(linkContent);
        }
        return link;
    }
}
