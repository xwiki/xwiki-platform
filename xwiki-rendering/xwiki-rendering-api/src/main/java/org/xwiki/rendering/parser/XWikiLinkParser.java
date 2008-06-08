package org.xwiki.rendering.parser;

import org.xwiki.rendering.listener.Link;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class XWikiLinkParser implements LinkParser
{
    // Implementation note: We're not using regex in general in order to provide better error
    // messages when throwing exceptions. In addition regex makes the code less readable.
    // FWIW this is the kind of regex that would need to be used:
    //   private static final Pattern LINK_PATTERN = Pattern.compile(
    //      "(?:([^\\|>]*)[\\|>])?([^\\|>]*)(?:@([^\\|>]*))?(?:[\\|>](.*))?");
    //   private static final Pattern REFERENCE_PATTERN = Pattern.compile(
    //      "(mailto:.*|http:.*)|(?:([^?#]*)[?#]?)?(?:([^#]*)[#]?)?(.*)?");

    private static final Pattern URL_SCHEME_PATTERN = Pattern.compile("[a-zA-Z0-9+.-]*://");

    private static final String MAILTO_URI_PREFIX = "mailto:";
    
    /**
     * Preferred separator for separating link parts (label, link and target).
     */
    private static final String LINK_SEPARATOR_GREATERTHAN = ">";

    /**
     * Other allowed separator for separating link parts (label, link and target).
     */
    private static final String LINK_SEPARATOR_PIPE = "|";

    public Link parse(String rawLink) throws ParseException
    {
        StringBuffer content = new StringBuffer(rawLink.trim());

        Link link = new Link();

        // Note: It's important to parse the label and the target in that order. See
        // {@link #parseLabel} for more details as to why.
        link.setLabel(parseLabel(content));
        link.setTarget(parseTarget(content));

        // Parse the link reference itself.
        String uri = parseURI(content);
        if (uri != null) {
            link.setDocumentNameOrUri(uri);
            link.setContainsDocumentName(false);
        } else {
            // Note: the order here is also very important.
            // We parse the query string early as it can contain our special delimiter characters
            // (like "."). Note: This means that "@" characters are forbidden in the query string...
            link.setInterWikiAlias(parseElementAfterString(content, "@"));
            link.setQueryString(parseElementAfterString(content, "?"));
        }

        link.setAnchor(parseElementAfterString(content, "#"));

        // What remains in the content buffer is the page name if any. If the content is empty then
        // it means no page was specified. This is allowed and in that case when the link is
        // rendered it'll be pointing to WebHome.
        if (content.length() > 0) {

            // TODO: Check for invalid characters in a page

            link.setDocumentNameOrUri(content.toString());
            link.setContainsDocumentName(true);
        }

        return link;
    }

    /**
     * Find out the label part of the full link.
     *
     * <p>Note: As it's possible to specify a target we need a way to differentiate the following
     * 2 links:
     * <ul>
     *   <li>[Web Home>_blank]  -> label = null, link = "Web Home", target = "_blank"</li>
     *   <li>[Web Home>WebHome] -> label = "Web Home", link = "WebHome", target = null</li>
     * </ul>
     * The rule we have chosen is to force targets to start with an underscore character ("_").
     * </p>
     *
     * @param content the string to parse. This parameter will be modified by the method to remove
     *                the parsed content.
     * @return the parsed label or null if no label was specified
     */
    protected String parseLabel(StringBuffer content)
    {
        String label = null;

        // A label only exists if there's a separator ("|" or ">").
        int separatorIndex = content.indexOf(LINK_SEPARATOR_PIPE);
        if (separatorIndex == -1) {
            separatorIndex = content.indexOf(LINK_SEPARATOR_GREATERTHAN);
        }

        if (separatorIndex != -1) {
            String text = content.substring(0, separatorIndex).trim();

            // Have we discovered a link or an label?
            if (content.charAt(separatorIndex + 1) != '_') {
                label = text;
                content.delete(0, separatorIndex + 1);
            }
        }

        return label;
    }

    /**
     * Find out the target part of the full link.
     *
     * <p>Note: The target element must start with an underscore ("_"). See
     * {@link #parseLabel(StringBuffer)} for more details as to why.</p>
     *
     * @param content the string to parse. This parameter will be modified by the method to remove
     *                the parsed content.
     * @return the parsed target or null if no target was specified
     * @throws ParseException if the target does not start with an underscore
     */
    protected String parseTarget(StringBuffer content) throws ParseException
    {
        String target = null;

        int separatorIndex = content.lastIndexOf(LINK_SEPARATOR_PIPE);
        if (separatorIndex == -1) {
            separatorIndex = content.lastIndexOf(LINK_SEPARATOR_GREATERTHAN);
        }

        if (separatorIndex != -1) {
            target = content.substring(separatorIndex + 1).trim();
            if (!target.startsWith("_")) {
                throw new ParseException("Invalid link format. The target element must start with "
                    + "an underscore, got [" + target + "]");
            }
            content.delete(separatorIndex, content.length());
        }

        return target;
    }

    /**
     * Find out the URI part of the full link. Supported URIs are either "mailto:" or any URL
     * in the form "protocol://".
     *
     * @param content the string to parse. This parameter will be modified by the method to remove
     *                the parsed content.
     * @return the parsed URI or null if no URI was specified
     * @throws ParseException if the URI is malformed
     */
    protected String parseURI(StringBuffer content) throws ParseException
    {
        String uri = null;

        // First, look for an email URI
        if (content.indexOf(MAILTO_URI_PREFIX) == 0) {
            try {
                uri = new URI(content.toString()).toString();
            } catch (URISyntaxException e) {
                throw new ParseException("Invalid mailto URI [" + content.toString() + "]", e);
            }
            content.setLength(0);
        } else {
            // Look for a URL pattern
            Matcher matcher = URL_SCHEME_PATTERN.matcher(content.toString());
            if (matcher.lookingAt()) {
                // If a URL is specified then virtual wiki aliases and spaces should not be allowed.
                try {
                    uri = new URL(content.toString()).toString();
                } catch (Exception e) {
                    throw new ParseException("Invalid URL format [" + content.toString() + "]", e);
                }
                content.setLength(0);
            }
        }

        return uri;
    }

    /**
     * Find out the element located to the right of the passed separator.
     *
     * @param content the string to parse. This parameter will be modified by the method to remove
     *                the parsed content.
     * @param separator the separator string to locate the element
     * @return the parsed element or null if the separator string wasn't found
     */
    protected String parseElementAfterString(StringBuffer content, String separator)
    {
        String element = null;

        int index = content.lastIndexOf(separator);
        if (index != -1) {
            element = content.substring(index + separator.length()).trim();
            content.delete(index, content.length());
        }

        return element;
    }
}
