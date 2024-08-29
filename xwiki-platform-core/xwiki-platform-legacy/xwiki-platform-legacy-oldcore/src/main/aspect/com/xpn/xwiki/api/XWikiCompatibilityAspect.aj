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
package com.xpn.xwiki.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryManager;
import org.xwiki.xml.XMLUtils;
import org.suigeneris.jrcs.diff.delta.Chunk;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;
import com.xpn.xwiki.stats.api.XWikiStatsService;
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiMessageTool;
import com.xpn.xwiki.internal.render.OldRendering;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.api.XWiki} class.
 *
 * @version $Id$
 */
public privileged aspect XWikiCompatibilityAspect
{
    /**
     * Utility methods have been moved in version 1.3 Milestone 2 to the {@link Util} class.
     * However to preserve backward compatibility we have deprecated them in this class and
     * not removed them yet. All calls are funnelled through this class variable.
     */
    private Util XWiki.util;

    /**
     * Capture the api.XWiki constructor so that we can initialize this.util.
     */
    private pointcut xwikiCreation(XWiki x): this(x) && execution( public com.xpn.xwiki.api.XWiki.new(..) );

    after(XWiki x): xwikiCreation(x)
    {
        x.util = new Util(x.xwiki, x.context);
    }

    /**
     * API to protect Text from Wiki transformation
     * @param text
     * @return escaped text
     * @deprecated replaced by Util#escapeText since 1.3M2
     */
    @Deprecated
    public String XWiki.escapeText(String text)
    {
        return this.util.escapeText(text);
    }

    /**
     * API to protect URLs from Wiki transformation
     * @param url
     * @return encoded URL
     * @deprecated replaced by Util#escapeURL since 1.3M2
     */
    @Deprecated
    public String XWiki.escapeURL(String url)
    {
        return this.util.escapeURL(url);
    }

    /**
     * @deprecated use {@link #getLanguagePreference()} instead
     */
    @Deprecated
    public String XWiki.getDocLanguagePreference()
    {
        return xwiki.getDocLanguagePreference(getXWikiContext());
    }

    /**
     * Privileged API to send a message to an email address
     *
     * @param sender email of the sender of the message
     * @param recipient email of the recipient of the message
     * @param message Message to send
     * @throws XWikiException if the mail was not send successfully
     * @deprecated replaced by the
     *   <a href="http://code.xwiki.org/xwiki/bin/view/Plugins/MailSenderPlugin">Mail Sender
     *   Plugin</a> since 1.3M2
     */
    @Deprecated
    public void XWiki.sendMessage(String sender, String recipient, String message)
        throws XWikiException
    {
        if (hasProgrammingRights())
            xwiki.sendMessage(sender, recipient, message, getXWikiContext());
    }

    /**
     * Privileged API to send a message to an email address
     *
     * @param sender email of the sender of the message
     * @param recipient emails of the recipients of the message
     * @param message Message to send
     * @throws XWikiException if the mail was not send successfully
     * @deprecated replaced by the
     *   <a href="http://code.xwiki.org/xwiki/bin/view/Plugins/MailSenderPlugin">Mail Sender
     *   Plugin</a> since 1.3M2
     */
    @Deprecated
    public void XWiki.sendMessage(String sender, String[] recipient, String message)
        throws XWikiException
    {
        if (hasProgrammingRights())
            xwiki.sendMessage(sender, recipient, message, getXWikiContext());
    }

    /**
     * @return the current date
     * @deprecated replaced by {@link com.xpn.xwiki.api.Util#getDate()} since 1.3M2
     */
    @Deprecated
    public Date XWiki.getCurrentDate()
    {
        return this.util.getDate();
    }

    /**
     * @return the current date
     * @deprecated replaced by {@link com.xpn.xwiki.api.Util#getDate()} since 1.3M2
     */
    @Deprecated
    public Date XWiki.getDate()
    {
        return this.util.getDate();
    }

    /**
     * @param time the time in milliseconds
     * @return the time delta in milliseconds between the current date and the time passed
     *         as parameter
     * @deprecated replaced by {@link com.xpn.xwiki.api.Util#getTimeDelta(long)} since 1.3M2
     */
    @Deprecated
    public int XWiki.getTimeDelta(long time)
    {
        return this.util.getTimeDelta(time);
    }

    /**
     * @param time time in milliseconds since 1970, 00:00:00 GMT
     * @return Date a date from a time in milliseconds since 01/01/1970 as a
     *         Java {@link Date} Object
     * @deprecated replaced by {@link com.xpn.xwiki.api.Util#getDate(long)} since 1.3M2
     */
    @Deprecated
    public Date XWiki.getDate(long time)
    {
        return this.util.getDate(time);
    }

    /**
     * Split a text to an array of texts, according to a separator.
     *
     * @param text the original text
     * @param sep the separator characters. The separator is one or more of the
     *        separator characters
     * @return An array containing the split text
     * @deprecated replaced by {@link com.xpn.xwiki.api.Util#split(String, String)} since 1.3M2
     */
    @Deprecated
    public String[] XWiki.split(String text, String sep)
    {
        return this.util.split(text, sep);
    }

    /**
     * Get a stack trace as a String
     *
     * @param e the exception to convert to a String
     * @return the exception stack trace as a String
     * @deprecated replaced by {@link com.xpn.xwiki.api.Util#printStrackTrace(Throwable)}
     *             since 1.3M2
     */
    @Deprecated
    public String XWiki.printStrackTrace(Throwable e)
    {
        return this.util.printStrackTrace(e);
    }

    /**
     * Get a Null object. This is useful in Velocity where there is no real null object
     * for comparaisons.
     *
     * @return a Null Object
     * @deprecated replaced by {@link Util#getNull()} since 1.3M2
     */
    @Deprecated
    public Object XWiki.getNull()
    {
        return this.util.getNull();
    }

    /**
     * Get a New Line character. This is useful in Velocity where there is no real new
     * line character for inclusion in texts.
     *
     * @return a new line character
     * @deprecated replaced by {@link com.xpn.xwiki.api.Util#getNewline()} since 1.3M2
     */
    @Deprecated
    public String XWiki.getNl()
    {
        return this.util.getNewline();
    }

    /**
     * Creates an Array List. This is useful from Velocity since you cannot
     * create Object from Velocity with our secure uberspector.
     *
     * @return a {@link ArrayList} object
     * @deprecated replaced by {@link com.xpn.xwiki.api.Util#getArrayList()} since 1.3M2
     */
    @Deprecated
    public List XWiki.getArrayList()
    {
        return this.util.getArrayList();
    }

    /**
     * Creates a Hash Map. This is useful from Velocity since you cannot
     * create Object from Velocity with our secure uberspector.
     *
     * @return a {@link HashMap} object
     * @deprecated replaced by {@link Util#getHashMap()} since 1.3M2
     */
    @Deprecated
    public Map XWiki.getHashMap()
    {
        return this.util.getHashMap();
    }

    /**
     * Creates a Tree Map. This is useful from Velocity since you cannot
     * create Object from Velocity with our secure uberspector.
     *
     * @return a {@link TreeMap} object
     * @deprecated replaced by {@link com.xpn.xwiki.api.Util#getTreeMap()} since 1.3M2
     */
    @Deprecated
    public Map XWiki.getTreeMap()
    {
        return this.util.getTreeMap();
    }

    /**
     * Sort a list using a standard comparator. Elements need to be mutally comparable and
     * implement the Comparable interface.
     *
     * @param list the list to sort
     * @return the sorted list (as the same oject reference)
     * @see {@link java.util.Collections#sort(java.util.List)}
     * @deprecated replaced by {@link com.xpn.xwiki.api.Util#sort(java.util.List)} since 1.3M2
     */
    @Deprecated
    public List XWiki.sort(List list)
    {
        return this.util.sort(list);
    }

    /**
     * Convert an Object to a number and return null if the object is not a Number.
     *
     * @param object the object to convert
     * @return the object as a {@link Number}
     * @deprecated replaced by {@link com.xpn.xwiki.api.Util#toNumber(Object)} since 1.3M2
     */
    @Deprecated
    public Number XWiki.toNumber(Object object)
    {
        return this.util.toNumber(object);
    }

    /**
     * Generate a random string.
     *
     * @param size the desired size of the string
     * @return the randomly generated string
     * @deprecated replaced by {@link com.xpn.xwiki.api.Util#generateRandomString(int)}
                   since 1.3M2
     */
    @Deprecated
    public String XWiki.generateRandomString(int size)
    {
        return this.util.generateRandomString(size);
    }

    /**
     * Output a BufferedImage object into the response outputstream.
     * Once this method has been called, not further action is possible.
     * Users should set $context.setFinished(true) to
     * avoid template output The image is outpout as image/jpeg.
     *
     * @param image the BufferedImage to output
     * @throws java.io.IOException if the output fails
     * @deprecated replaced by
     *             {@link com.xpn.xwiki.api.Util#outputImage(java.awt.image.BufferedImage)}
     *             since 1.3M2
     */
    @Deprecated
    public void XWiki.outputImage(BufferedImage image) throws IOException
    {
        this.util.outputImage(image);
    }

    /**
     * Returns the recently visited pages for a specific action
     *
     * @param action ("view" or "edit")
     * @param size how many recent actions to retrieve
     * @return a ArrayList of document names
     * @deprecated use {@link #getStatsService()} instead
     */
    @Deprecated
    public java.util.Collection XWiki.getRecentActions(String action, int size)
    {
        XWikiStatsService stats = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (stats == null)
            return Collections.EMPTY_LIST;
        return stats.getRecentActions(action, size, getXWikiContext());
    }

    /**
     * @param str the String to convert to an integer
     * @return the parsed integer or zero in case of exception
     * @deprecated replaced by {@link Util#parseInt(String)} since 1.3M2
     */
    @Deprecated
    public int XWiki.parseInt(String str)
    {
        return this.util.parseInt(str);
    }

    /**
     * @param str the String to convert to an Integer Object
     * @return the parsed integer or zero in case of exception
     * @deprecated replaced by {@link Util#parseInteger(String)} since 1.3M2
     */
    @Deprecated
    public Integer XWiki.parseInteger(String str)
    {
        return this.util.parseInteger(str);
    }

    /**
     * @param str the String to convert to a long
     * @return the parsed long or zero in case of exception
     * @deprecated replaced by {@link Util#parseLong(String)} since 1.3M2
     */
    @Deprecated
    public long XWiki.parseLong(String str)
    {
        return this.util.parseLong(str);
    }

    /**
     * @param str the String to convert to a float
     * @return the parsed float or zero in case of exception
     * @deprecated replaced by {@link Util#parseFloat(String)} since 1.3M2
     */
    @Deprecated
    public float XWiki.parseFloat(String str)
    {
        return this.util.parseFloat(str);
    }

    /**
     * @param str the String to convert to a double
     * @return the parsed double or zero in case of exception
     * @deprecated replaced by {@link Util#parseDouble(String)} since 1.3M2
     */
    @Deprecated
    public double XWiki.parseDouble(String str)
    {
        return this.util.parseDouble(str);
    }

    /**
     * Escape text so that it can be used in a like clause or in a test for equality clause.
     * For example it escapes single quote characters.
     *
     * @param text the text to escape
     * @return filtered text
     * @deprecated replaced by {@link Util#escapeSQL(String)} since 1.3M2
     */
    @Deprecated
    public String XWiki.sqlfilter(String text)
    {
        return this.util.escapeSQL(text);
    }

    /**
     * Cleans up the passed text by removing all accents and special characters to make it
     * a valid page name.
     *
     * @param name the page name to normalize
     * @return the valid page name
     * @deprecated replaced by {@link Util#clearName(String)} since 1.3M2
     */
    @Deprecated
    public String XWiki.clearName(String name)
    {
        return this.util.clearName(name);
    }

    /**
     * Replace all accents by their alpha equivalent.
     *
     * @param text the text to parse
     * @return a string with accents replaced with their alpha equivalent
     * @deprecated replaced by {@link Util#clearAccents(String)} since 1.3M2
     */
    @Deprecated
    public String XWiki.clearAccents(String text)
    {
        return this.util.clearAccents(text);
    }

    /**
     * Add a and b because Velocity operations are not always working.
     *
     * @param a an integer to add
     * @param b an integer to add
     * @return the sum of a and b
     * @deprecated replaced by {@link Util#add(int, int)} since 1.3M2
     */
    @Deprecated
    public int XWiki.add(int a, int b)
    {
        return this.util.add(a, b);
    }

    /**
     * Add a and b because Velocity operations are not working with longs.
     *
     * @param a a long to add
     * @param b a long to add
     * @return the sum of a and b
     * @deprecated replaced by {@link Util#add(long, long)} since 1.3M2
     */
    @Deprecated
    public long XWiki.add(long a, long b)
    {
        return this.util.add(a, b);
    }

    /**
     * Add a and b where a and b are non decimal numbers specified as Strings.
     *
     * @param a a string representing a non decimal number
     * @param b a string representing a non decimal number
     * @return the sum of a and b as a String
     * @deprecated replaced by {@link Util#add(String, String)} since 1.3M2
     */
    @Deprecated
    public String XWiki.add(String a, String b)
    {
        return this.util.add(a,  b);
    }

    /**
     * Transform a text in a URL compatible text
     * 
     * @param content text to transform
     * @return encoded result
     * @deprecated replaced by {@link Util#encodeURI(String)} since 1.3M2
     */
    @Deprecated
    public String XWiki.getURLEncoded(String content)
    {
        return this.util.encodeURI(content);
    }
    
    /**
     * @return true for multi-wiki/false for mono-wiki
     * @deprecated replaced by {@link XWiki#isVirtualMode()} since 1.4M1.
     */
    @Deprecated
    public boolean XWiki.isVirtual()
    {
        return this.isVirtualMode();
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpaceCopyright()} since 2.3M1
     */
    @Deprecated
    public String XWiki.getWebCopyright()
    {
        return this.getSpaceCopyright();
    }

    /**
     * Get the reference of the space and fallback on parent space or wiki in case nothing is found.
     * <p>
     * If the property is not set on any level then empty String is returned.
     *
     * @param preference Preference name
     * @param space The space for which this preference is requested
     * @return The preference for this wiki and the current locale
     * @deprecated use {@link #getSpacePreferenceFor(String, SpaceReference)} instead
     */
    @Deprecated(since = "14.8RC1")
    public String XWiki.getSpacePreferenceFor(String preference, String space)
    {
        return getSpacePreferenceFor(preference, space, "");
    }

    /**
     * Get the reference of the space and fallback on parent space or wiki in case nothing is found.
     * <p>
     * If the property is not set on any level then <code>defaultValue</code> is returned.
     *
     * @param preference Preference name
     * @param space The space for which this preference is requested
     * @param defaultValue default value to return if the preference does not exist or is empty
     * @return The preference for this wiki and the current locale in long format
     * @deprecated use {@link #getSpacePreferenceFor(String, SpaceReference, String)} instead
     */
    @Deprecated(since = "14.8RC1")
    public String XWiki.getSpacePreferenceFor(String preference, String space, String defaultValue)
    {
        return this.xwiki.getSpacePreference(preference, space, defaultValue, getXWikiContext());
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreference(String)} since 2.3M1
     */
    @Deprecated
    public String XWiki.getWebPreference(String preference)
    {
        return this.getSpacePreference(preference);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreferenceFor(String, String)} since 2.3M1
     */
    @Deprecated
    public String XWiki.getWebPreferenceFor(String preference, String space)
    {
        return this.getSpacePreferenceFor(preference, space);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreferenceFor(String, String, String)} since 2.3M1
     */
    @Deprecated
    public String XWiki.getWebPreferenceFor(String preference, String space, String defaultValue)
    {
        return this.getSpacePreferenceFor(preference, space, defaultValue);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreferenceAsLong(String, long)} since 2.3M1
     */
    @Deprecated
    public long XWiki.getWebPreferenceAsLong(String preference, long defaultValue)
    {
        return this.getSpacePreferenceAsLong(preference, defaultValue);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreferenceAsLong(String)} since 2.3M1
     */
    @Deprecated
    public long XWiki.getWebPreferenceAsLong(String preference)
    {
        return this.getSpacePreferenceAsLong(preference);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreferenceAsInt(String, int)} since 2.3M1
     */
    @Deprecated
    public int XWiki.getWebPreferenceAsInt(String preference, int defaultValue)
    {
        return this.getSpacePreferenceAsInt(preference, defaultValue);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreference(String, String)} since 2.3M1
     */
    @Deprecated
    public String XWiki.getWebPreference(String preference, String defaultValue)
    {
        return this.getSpacePreference(preference, defaultValue);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreferenceAsInt(String)} since 2.3M1
     */
    @Deprecated
    public int XWiki.getWebPreferenceAsInt(String preference)
    {
        return this.getSpacePreferenceAsInt(preference);
    }

    /**
     * @deprecated replaced by {@link XWiki#copySpaceBetweenWikis(String, String, String, String, boolean)} since 2.3M1
     */
    @Deprecated
    public int XWiki.copyWikiWeb(String space, String sourceWiki, String targetWiki, String language, boolean clean)
        throws XWikiException
    {
        return this.copySpaceBetweenWikis(space, sourceWiki, targetWiki, language, clean);
    }

    /**
     * API to parse a message. A message can be an error message or an information message either as text or as a message
     * ID pointing to ApplicationResources. The message is also parse for velocity scripts
     * 
     * @return Final message
     * @param id
     * @return the result of the parsed message
     * @deprecated use {@link XWikiMessageTool#get(String, List)} instead. From velocity you can access XWikiMessageTool
     *             with $msg binding.
     */
    @Deprecated
    public String XWiki.parseMessage(String id)
    {
        return this.xwiki.parseMessage(id, getXWikiContext());
    }

    /**
     * API to get a message. A message can be an error message or an information message either as text or as a message
     * ID pointing to ApplicationResources. The message is also parsed for velocity scripts
     * 
     * @return Final message
     * @param id
     * @return the result of the parsed message
     * @deprecated use {@link XWikiMessageTool#get(String, List)} instead. From velocity you can access XWikiMessageTool
     *             with $msg binding.
     */
    @Deprecated
    public String XWiki.getMessage(String id)
    {
        return this.xwiki.getMessage(id, getXWikiContext());
    }

    /**
     * Transform a text in a form compatible text
     * 
     * @param content text to transform
     * @return encoded result
     * @deprecated Use $escapetool.xml
     */
    @Deprecated
    public String XWiki.getFormEncoded(String content)
    {
        return XMLUtils.escape(content);
    }

    /**
     * Transform a text in a XML compatible text This method uses Apache CharacterFilter which swaps single quote
     * (&#39;) for left single quotation mark (&#8217;)
     * 
     * @param content text to transform
     * @return encoded result
     * @deprecated Use $escapetool.xml
     */
    @Deprecated
    public String XWiki.getXMLEncoded(String content)
    {
        return XMLUtils.escape(content);
    }

    /**
     * Output content in the edit content htmlarea
     * 
     * @param content content to output
     * @return the htmlarea text content
     * @deprecated Removed since it isn't used; since 3.1M2.
     */
    @Deprecated
    public String XWiki.getHTMLArea(String content)
    {
        return this.xwiki.getHTMLArea(content, getXWikiContext());
    }
    

    /**
     * API to display a select box for the list of available field for a specific class This field data can then be used
     * to generate an XWiki Query showing a table with the relevant data
     * 
     * @param className XWiki Class Name to display the list of columns for
     * @param query Query to pre-select the currently selected columns
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    @Deprecated
    public String XWiki.displaySearchColumns(String className, XWikiQuery query) throws XWikiException
    {
        return this.xwiki.displaySearchColumns(className, "", query, getXWikiContext());
    }

    /**
     * API to display a select box for the list of available field for a specific class, optionally adding a prefix This
     * field data can then be used to generate an XWiki Query showing a table with the relevant data
     * 
     * @param className XWiki Class Name to display the list of columns for
     * @param prefix Prefix to add to the field name
     * @param query Query to pre-select the currently selected columns
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    @Deprecated
    public String XWiki.displaySearchColumns(String className, String prefix, XWikiQuery query) throws XWikiException
    {
        return this.xwiki.displaySearchColumns(className, prefix, query, getXWikiContext());
    }

    /**
     * API to display a select box for the list of available field for a specific class This field data can then be used
     * to generate the order element of an XWiki Query showing a table with the relevant data
     * 
     * @param className XWiki Class Name to display the list of columns for
     * @param query Query to pre-select the currently selected columns
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    @Deprecated
    public String XWiki.displaySearchOrder(String className, XWikiQuery query) throws XWikiException
    {
        return this.xwiki.displaySearchOrder(className, "", query, getXWikiContext());
    }

    /**
     * API to display a select box for the list of available field for a specific class, optionally adding a prefix This
     * field data can then be used to generate the order element of an XWiki Query showing a table with the relevant
     * data
     * 
     * @param className XWiki Class Name to display the list of columns for
     * @param prefix Prefix to add to the field name
     * @param query Query to pre-select the currently selected columns
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    @Deprecated
    public String XWiki.displaySearchOrder(String className, String prefix, XWikiQuery query) throws XWikiException
    {
        return this.xwiki.displaySearchOrder(className, prefix, query, getXWikiContext());
    }

    /**
     * API to run a search from an XWikiQuery Object An XWikiQuery object can be created from a request using the
     * createQueryFromRequest function
     * 
     * @param query query to run the search for
     * @return A list of document names matching the query
     * @throws XWikiException exception is a failure occured
     */
    public <T> List<T> XWiki.search(XWikiQuery query) throws XWikiException
    {
        return this.xwiki.search(query, getXWikiContext());
    }

    /**
     * API to create a query from a request Object The request object is the result of a form created from the
     * displaySearch() and displaySearchColumns() functions
     * 
     * @param className class name to create the query from
     * @return an XWikiQuery object matching the selected values in the request object
     * @throws XWikiException exception is a failure occured
     */
    public XWikiQuery XWiki.createQueryFromRequest(String className) throws XWikiException
    {
        return this.xwiki.createQueryFromRequest(className, getXWikiContext());
    }

    /**
     * API to run a search from an XWikiQuery Object and display it as a HTML table An XWikiQuery object can be created
     * from a request using the createQueryFromRequest function
     * 
     * @param query query to run the search for
     * @return An HTML table showing the result
     * @throws XWikiException exception is a failure occured
     */
    public String XWiki.searchAsTable(XWikiQuery query) throws XWikiException
    {
        return this.xwiki.searchAsTable(query, getXWikiContext());
    }

    /**
     * API to display a field in search mode for a specific class with preselected values This field data can then be
     * used to generate an XWiki Query showing a table with the relevant data
     * 
     * @param fieldname field name in the class
     * @param className class name to display the field from
     * @param criteria XWikiCriteria object (usually the XWikiQuery object) to take the preselected values from
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String XWiki.displaySearch(String fieldname, String className, XWikiCriteria criteria) throws XWikiException
    {
        return this.xwiki.displaySearch(fieldname, className, criteria, getXWikiContext());
    }

    /**
     * API to display a field in search mode for a specific class with preselected values, optionally adding a prefix to
     * the field name This field data can then be used to generate an XWiki Query showing a table with the relevant data
     * 
     * @param fieldname field name in the class
     * @param className class name to display the field from
     * @param prefix prefix to add to the field name
     * @param criteria XWikiCriteria object (usually the XWikiQuery object) to take the preselected values from
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String XWiki.displaySearch(String fieldname, String className, String prefix, XWikiCriteria criteria)
        throws XWikiException
    {
        return this.xwiki.displaySearch(fieldname, className, prefix, criteria, getXWikiContext());
    }

    /**
     * API to display a field in search mode for a specific class without preselected values This field data can then be
     * used to generate an XWiki Query showing a table with the relevant data
     * 
     * @param fieldname field name in the class
     * @param className class name to display the field from
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String XWiki.displaySearch(String fieldname, String className) throws XWikiException
    {
        return this.xwiki.displaySearch(fieldname, className, getXWikiContext());
    }

    /**
     * Priviledged API to create a new Wiki from an existing wiki This creates the database, copies to documents from a
     * existing wiki Assigns the admin rights, creates the Wiki identification page in the main wiki
     * 
     * @param wikiName Wiki Name to create
     * @param wikiUrl Wiki URL to accept requests from
     * @param wikiAdmin Wiki admin user
     * @param baseWikiName Wiki to copy documents from
     * @param failOnExist true to fail if the wiki already exists, false to overwrite
     * @return Success of Failure code (0 for success, -1 for missing programming rights, > 0 for other errors
     * @throws XWikiException
     * @deprecated use WikiManager plugin instead
     */
    @Deprecated
    public int XWiki.createNewWiki(String wikiName, String wikiUrl, String wikiAdmin, String baseWikiName, boolean failOnExist)
        throws XWikiException
    {
        return createNewWiki(wikiName, wikiUrl, wikiAdmin, baseWikiName, "", null, failOnExist);
    }

    /**
     * Priviledged API to create a new Wiki from an existing wiki This creates the database, copies to documents from a
     * existing wiki Assigns the admin rights, creates the Wiki identification page in the main wiki
     * 
     * @param wikiName Wiki Name to create
     * @param wikiUrl Wiki URL to accept requests from
     * @param wikiAdmin Wiki admin user
     * @param baseWikiName Wiki to copy documents from
     * @param description Description of the Wiki
     * @param failOnExist true to fail if the wiki already exists, false to overwrite
     * @return Success of Failure code (0 for success, -1 for missing programming rights, > 0 for other errors
     * @throws XWikiException
     * @deprecated use WikiManager plugin instead
     */
    @Deprecated
    public int XWiki.createNewWiki(String wikiName, String wikiUrl, String wikiAdmin, String baseWikiName,
        String description, boolean failOnExist) throws XWikiException
    {
        return createNewWiki(wikiName, wikiUrl, wikiAdmin, baseWikiName, description, null, failOnExist);
    }

    /**
     * Priviledged API to create a new Wiki from an existing wiki This creates the database, copies to documents from a
     * existing wiki Assigns the admin rights, creates the Wiki identification page in the main wiki Copy is limited to
     * documents of a specified language. If a document for the language is not found, the default language document is
     * used
     * 
     * @param wikiName Wiki Name to create
     * @param wikiUrl Wiki URL to accept requests from
     * @param wikiAdmin Wiki admin user
     * @param baseWikiName Wiki to copy documents from
     * @param description Description of the Wiki
     * @param language Language to copy
     * @param failOnExist true to fail if the wiki already exists, false to overwrite
     * @return Success of Failure code (0 for success, -1 for missing programming rights, > 0 for other errors
     * @throws XWikiException
     * @deprecated use WikiManager plugin instead
     */
    @Deprecated
    public int XWiki.createNewWiki(String wikiName, String wikiUrl, String wikiAdmin, String baseWikiName,
        String description, String language, boolean failOnExist) throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.xwiki.createNewWiki(wikiName, wikiUrl, wikiAdmin, baseWikiName, description, language,
                failOnExist, getXWikiContext());
        }

        return -1;
    }

    /**
     * API to parse the message being stored in the Context. A message can be an error message or an information message
     * either as text or as a message ID pointing to ApplicationResources. The message is also parse for velocity
     * scripts
     * 
     * @return Final message
     * @deprecated use {@link org.xwiki.localization.LocalizationManager} instead. From velocity you can access it
     *             using the {@code $services.localization} binding, see {@code LocalizationScriptService}
     */
    @Deprecated
    public String XWiki.parseMessage()
    {
        return this.xwiki.parseMessage(getXWikiContext());
    }

    /**
     * API allowing to count the total number of documents that would be returned by a query.
     * 
     * @param wheresql Query to use, similar to the ones accepted by {@link #searchDocuments(String)}. If possible, it
     *            should not contain <code>order by</code> or <code>group</code> clauses, since this kind of queries are
     *            not portable.
     * @return The number of documents that matched the query.
     * @throws XWikiException if there was a problem executing the query.
     * @deprecated use query service instead
     */
    @Deprecated
    public int XWiki.countDocuments(String wheresql) throws XWikiException
    {
        return this.xwiki.getStore().countDocuments(wheresql, getXWikiContext());
    }

    /**
     * API allowing to count the total number of documents that would be returned by a parameterized query.
     * 
     * @param parameterizedWhereClause the parameterized query to use, similar to the ones accepted by
     *            {@link #searchDocuments(String, List)}. If possible, it should not contain <code>order by</code> or
     *            <code>group</code> clauses, since this kind of queries are not portable.
     * @param parameterValues The parameter values that replace the question marks.
     * @return The number of documents that matched the query.
     * @throws XWikiException if there was a problem executing the query.
     * @deprecated use query service instead
     */
    @Deprecated
    public int XWiki.countDocuments(String parameterizedWhereClause, List< ? > parameterValues) throws XWikiException
    {
        return this.xwiki.getStore().countDocuments(parameterizedWhereClause, parameterValues, getXWikiContext());
    }
    

    /**
     * Privileged API allowing to run a search on the database returning a list of data This search is send to the store
     * engine (Hibernate HQL, JCR XPATH or other).
     * 
     * @param wheresql Query to be run (HQL, XPath)
     * @return A list of rows (Object[])
     * @throws XWikiException
     * @deprecated use query service instead
     */
    @Deprecated
    public <T> List<T> XWiki.search(String wheresql) throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.xwiki.search(wheresql, getXWikiContext());
        }

        return Collections.emptyList();
    }

    /**
     * Privileged API allowing to run a search on the database returning a list of data. The HQL where clause uses
     * parameters (question marks) instead of values, and the actual values are passed in the parameters list. This
     * allows generating a query which will automatically encode the passed values (like escaping single quotes). This
     * API is recommended to be used over the other similar methods where the values are passed inside the where clause
     * and for which manual encoding/escaping is needed to avoid SQL injections or bad queries.
     * 
     * @param parameterizedWhereClause query to be run (HQL)
     * @param parameterValues the where clause values that replace the question marks
     * @return a list of rows, where each row has either the selected data type ({@link XWikiDocument}, {@code String},
     *         {@code Integer}, etc.), or {@code Object[]} if more than one column was selected
     * @throws XWikiException
     * @deprecated use query service instead
     */
    @Deprecated
    public <T> List<T> XWiki.search(String parameterizedWhereClause, List< ? > parameterValues) throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.xwiki.getStore().search(parameterizedWhereClause, 0, 0, parameterValues, getXWikiContext());
        }

        return Collections.emptyList();
    }

    /**
     * Privileged API allowing to run a search on the database returning a list of data. This search is sent to the
     * store engine (Hibernate HQL, JCR XPATH or other)
     * 
     * @param wheresql Query to be run (HQL, XPath)
     * @param nb return only 'nb' rows
     * @param start skip the 'start' first elements
     * @return A list of rows (Object[])
     * @throws XWikiException
     * @deprecated use query service instead
     */
    @Deprecated
    public <T> List<T> XWiki.search(String wheresql, int nb, int start) throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.xwiki.search(wheresql, nb, start, getXWikiContext());
        }

        return Collections.emptyList();
    }

    /**
     * Privileged API allowing to run a search on the database returning a list of data. The HQL where clause uses
     * parameters (question marks) instead of values, and the actual values are passed in the paremeters list. This
     * allows generating a query which will automatically encode the passed values (like escaping single quotes). This
     * API is recommended to be used over the other similar methods where the values are passed inside the where clause
     * and for which manual encoding/escaping is needed to avoid sql injections or bad queries.
     * 
     * @param parameterizedWhereClause query to be run (HQL)
     * @param maxResults maximum number of results to return; if 0 all results are returned
     * @param startOffset skip the first N results; if 0 no items are skipped
     * @param parameterValues the where clause values that replace the question marks
     * @return a list of rows, where each row has either the selected data type ({@link XWikiDocument}, {@code String},
     *         {@code Integer}, etc.), or {@code Object[]} if more than one column was selected
     * @throws XWikiException
     * @deprecated use query service instead
     */
    @Deprecated
    public <T> List<T> XWiki.search(String parameterizedWhereClause, int maxResults, int startOffset,
        List< ? > parameterValues) throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.xwiki.getStore().search(parameterizedWhereClause, maxResults, startOffset, parameterValues,
                getXWikiContext());
        }

        return Collections.emptyList();
    }

    /**
     * Deprecated API which was retrieving the SQL to represent the fullName Document field depending on the database
     * used This is not needed anymore and returns 'doc.fullName' for all databases
     * 
     * @deprecated
     * @return "doc.fullName"
     */
    @Deprecated
    public String XWiki.getFullNameSQL()
    {
        return this.xwiki.getFullNameSQL();
    }

    /**
     * @return secure {@link QueryManager} for execute queries to store.
     * @deprecated since XE 2.4M2 use the Query Manager Script Service
     */
    @Deprecated
    public QueryManager XWiki.getQueryManager()
    {
        return Utils.getComponent(QueryManager.class, "secure");
    }

    /**
     * API to check if wiki is in multi-wiki mode (virtual)
     * 
     * @deprecated Virtual mode is on by default, starting with XWiki 5.0M2.
     * @return true for multi-wiki/false for mono-wiki
     */
    @Deprecated
    public boolean XWiki.isVirtualMode()
    {
        return this.xwiki.isVirtualMode();
    }

    /**
     * API to access the current starts for the Wiki for a specific action It retrieves the number of times the action
     * was performed for the whole wiki The statistics module need to be activated (xwiki.stats=1 in xwiki.cfg)
     * 
     * @param action action for which to retrieve statistics (view/save/download)
     * @return A DocumentStats object with number of actions performed, unique visitors, number of visits
     * @deprecated use {@link #getStatsService()} instead
     */
    @Deprecated
    public DocumentStats XWiki.getCurrentMonthXWikiStats(String action)
    {
        return this.xwiki.getStatsService(getXWikiContext())
            .getDocMonthStats("", action, new Date(), getXWikiContext());
    }

    /**
     * Privileged API to reset the rendering engine This would restore the rendering engine evaluation loop and take
     * into account new configuration parameters
     * 
     * @deprecated
     */
    @Deprecated
    public void XWiki.resetRenderingEngine()
    {
        if (hasProgrammingRights()) {
            try {
                this.xwiki.resetRenderingEngine(getXWikiContext());
            } catch (XWikiException e) {
            }
        }
    }

    /**
     * API to render a text in the context of a document. Only works for xwiki/1.0 content.
     *
     * @param text text to render
     * @param doc the text is evaluated in the content of this document
     * @return evaluated content
     * @throws XWikiException if the evaluation went wrong
     * @deprecated
     */
    @Deprecated
    public String XWiki.renderText(String text, Document doc) throws XWikiException
    {
        return Utils.getComponent(OldRendering.class).renderText(text, doc.getDoc(), getXWikiContext());
    }

    /**
     * API to render a chunk (difference between two versions
     *
     * @param chunk difference between versions to render
     * @param doc document to use as a context for rendering
     * @return resuilt of the rendering
     * @deprecated
     */
    @Deprecated
    public String XWiki.renderChunk(Chunk chunk, Document doc)
    {
        return renderChunk(chunk, false, doc);
    }

    /**
     * API to render a chunk (difference between two versions
     *
     * @param chunk difference between versions to render
     * @param doc document to use as a context for rendering
     * @param source true to render the difference as wiki source and not as wiki rendered text
     * @return resuilt of the rendering
     * @deprecated
     */
    @Deprecated
    public String XWiki.renderChunk(Chunk chunk, boolean source, Document doc)
    {
        StringBuffer buf = new StringBuffer();
        chunk.toString(buf, "", "\n");
        if (source == true) {
            return buf.toString();
        }

        try {
            return renderText(buf.toString(), doc);
        } catch (Exception e) {
            return buf.toString();
        }
    }

    /**
     * API to rename a page (experimental) Rights are necessary to edit the source and target page All objects and
     * attachments ID are modified in the process to link to the new page name
     *
     * @param doc page to rename
     * @param newFullName target page name to move the information to
     * @deprecated since 12.0RC1. Use {@link Document#rename(DocumentReference)}.
     */
    @Deprecated
    public boolean XWiki.renamePage(Document doc, String newFullName)
    {
        try {
            if (this.xwiki.exists(newFullName, getXWikiContext()) && !this.xwiki.getRightService()
                .hasAccessLevel("delete", getXWikiContext().getUser(), newFullName, getXWikiContext())) {
                return false;
            }
            if (this.xwiki.getRightService().hasAccessLevel("edit", getXWikiContext().getUser(), doc.getFullName(),
                getXWikiContext())) {
                this.xwiki.renamePage(doc.getFullName(), newFullName, getXWikiContext());
            }
        } catch (XWikiException e) {
            return false;
        }

        return true;
    }

    /**
     * @return the ids of configured syntaxes for this wiki (e.g. {@code xwiki/2.0}, {@code xwiki/2.1},
     *         {@code mediawiki/1.0}, etc), taken only from {@code xwiki.cfg} (using the
     *         {@code xwiki.rendering.syntaxes} property)
     * @deprecated since 8.2M1, use the XWiki Rendering Configuration component or the Rendering Script Service one
     *             instead (they use a more elaborate algorithm to find out the supported syntaxes)
     */
    @Deprecated
    public List<String> XWiki.getConfiguredSyntaxes()
    {
        return this.xwiki.getConfiguredSyntaxes();
    }

    /**
     * Designed to include dynamic content, such as Servlets or JSPs, inside Velocity templates; works by creating a
     * RequestDispatcher, buffering the output, then returning it as a string.
     *
     * @param url URL of the servlet
     * @return text result of the servlet
     * @deprecated since 12.10.9, 13.4.3, 13.7RC1
     */
    @Deprecated
    public String XWiki.invokeServletAndReturnAsString(String url)
    {
        return hasProgrammingRights() ? this.xwiki.invokeServletAndReturnAsString(url, getXWikiContext()) : null;
    }

    /**
     * Inserts a tooltip using toolTip.js
     *
     * @param html HTML viewed
     * @param message HTML Tooltip message
     * @param params Parameters in Javascropt added to the tooltip config
     * @return HTML with working tooltip
     * @deprecated since 16.0RC1, this method doesn't work for a long time since flamingo skin
     */
    @Deprecated(since = "16.0RC1")
    public String XWiki.addTooltip(String html, String message, String params)
    {
        return this.xwiki.addTooltip(html, message, params, getXWikiContext());
    }

    /**
     * Inserts a tooltip using toolTip.js
     *
     * @param html HTML viewed
     * @param message HTML Tooltip message
     * @return HTML with working tooltip
     * @deprecated since 16.0RC1, this method doesn't work for a long time since flamingo skin
     */
    @Deprecated(since = "16.0RC1")
    public String XWiki.addTooltip(String html, String message)
    {
        return this.xwiki.addTooltip(html, message, getXWikiContext());
    }

    /**
     * Inserts the tooltip Javascript
     *
     * @return
     * @deprecated since 16.0RC1, this method doesn't work for a long time since flamingo skin
     */
    @Deprecated(since = "16.0RC1")
    public String XWiki.addTooltipJS()
    {
        return this.xwiki.addTooltipJS(getXWikiContext());
    }
}