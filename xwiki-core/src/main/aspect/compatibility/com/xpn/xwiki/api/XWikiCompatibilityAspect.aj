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
package com.xpn.xwiki.api;

import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.api.Util;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.stats.api.XWikiStatsService;

import java.io.IOException;
import java.awt.image.BufferedImage;
import java.util.*;

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
    private pointcut xwikiCreation(XWiki x): this(x) && execution( public XWiki.new(..) );

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
     * @see #getExoService(String)
     * @deprecated use {@link #getExoService(String)} instead
     */
    @Deprecated
    public java.lang.Object XWiki.getService(String className) throws XWikiException
    {
        return getExoService(className);
    }

    /**
     * @see #getExoPortalService(String)
     * @deprecated use {@link #getExoPortalService(String)} instead
     */
    @Deprecated
    public java.lang.Object XWiki.getPortalService(String className) throws XWikiException
    {
        return getExoPortalService(className);
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
     * API to parse the message being stored in the Context. A message can be an error message or an information message
     * either as text or as a message ID pointing to ApplicationResources. The message is also parse for velocity scripts
     * 
     * @return Final message
     * @deprecated use {@link XWikiMessageTool#get(String, List)} instead. From velocity you can access XWikiMessageTool
     *             with $msg binding.
     */
    @Deprecated
    public String XWiki.parseMessage()
    {
        return this.xwiki.parseMessage(getXWikiContext());
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
}