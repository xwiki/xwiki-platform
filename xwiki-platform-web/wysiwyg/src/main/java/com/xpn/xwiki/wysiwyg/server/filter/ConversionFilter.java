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
package com.xpn.xwiki.wysiwyg.server.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.wysiwyg.server.cleaner.HTMLCleaner;
import com.xpn.xwiki.wysiwyg.server.converter.HTMLConverter;

/**
 * This filter is used to convert the values of request parameters that hold WYSIWYG output from HTML to the storing
 * syntax (the syntax in which the content is stored, usually a wiki syntax). This is needed because the action
 * processing the request expects storing syntax and not HTML code in these request parameters. The conversion is done
 * using the new rendering module. It has to be done on the server and not on the client, like the old WYSIWYG editor
 * does. Doing the conversion on the client side by making an asynchronous request to the server is error-prone for the
 * following reason: the WYSIWYG behaves like a text area that can be put anywhere in an HTML page, inside or outside an
 * HTML form; because of this the editor is not aware of what submit buttons are present on the container page and what
 * submit logic these buttons might have associated with them.
 * 
 * @version $Id$
 */
public class ConversionFilter implements Filter
{
    /**
     * The logger instance.
     */
    private static final Log LOG = LogFactory.getLog(ConversionFilter.class);

    /**
     * The name of the request parameter holding the list of WYSIWYG editor names. Each WYSIWYG editor has its own name
     * which is also a request parameter holding the content of that editor. The name of a WYSIWYG editor is also used
     * as a prefix for other request parameters like syntax.
     */
    private static final String WYSIWYG_NAME = "wysiwyg";

    /**
     * The name of the session attribute holding the map with the data that is about to be displayed in different
     * WYSIWYG editors. An editor gets the key to its data through configuration. A key is a random string. Each data
     * should be removed from the map after it is displayed.<br/>
     * This map is needed to keep user changes while switching editors and after server-side exceptions.
     */
    private static final String WYSIWYG_INPUT = "com.xpn.xwiki.wysiwyg.input";

    /**
     * The name of the session attribute holding the map with the recent server-side exceptions regarding the WYSIWYG
     * editors. This filter uses this attribute to pass to the editors the exceptions caught during conversion. Each
     * exception should be removed from the map after being displayed to the user. The key in this map is a random
     * string and might match a key in the {@link #WYSIWYG_INPUT} map.
     */
    private static final String WYSIWYG_ERROR = "com.xpn.xwiki.wysiwyg.error";

    /**
     * {@inheritDoc}
     * 
     * @see Filter#destroy()
     */
    public void destroy()
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
        ServletException
    {
        String[] wysiwygNames = req.getParameterValues(WYSIWYG_NAME);
        if (wysiwygNames != null) {
            MutableServletRequestFactory mreqFactory = (MutableServletRequestFactory) Utils.getComponent(
                MutableServletRequestFactory.class, req.getProtocol());
            MutableServletRequest mreq = mreqFactory.newInstance(req);
            // Remove the list of WYSIWYG names from this request to avoid recurrency.
            mreq.removeParameter(WYSIWYG_NAME);
            Throwable[] errors = new Throwable[wysiwygNames.length];
            boolean sendBack = false;
            for (int i = 0; i < wysiwygNames.length; i++) {
                String wysiwygName = wysiwygNames[i];
                if (StringUtils.isEmpty(wysiwygName)) {
                    continue;
                }
                // Remove the syntax parameter from this request to avoid interference with further request processing.
                String syntax = mreq.removeParameter(wysiwygName + "_syntax");
                try {
                    HTMLCleaner cleaner = (HTMLCleaner) Utils.getComponent(HTMLCleaner.class);
                    HTMLConverter converter = (HTMLConverter) Utils.getComponent(HTMLConverter.class, syntax);
                    mreq.setParameter(wysiwygName, converter.fromHTML(cleaner.clean(req.getParameter(wysiwygName))));
                } catch (Throwable t) {
                    LOG.error(t.getMessage(), t);
                    sendBack = true;
                    errors[i] = t;
                }
            }

            if (sendBack) {
                String referer = StringUtils.substringBeforeLast(mreq.getReferer(), String.valueOf('?'));
                String queryString = StringUtils.substringAfterLast(mreq.getReferer(), String.valueOf('?'));
                // Remove previous keys from the query string. We have to do this since this might not be the first time
                // the conversion fails for this referrer.
                queryString = queryString.replaceAll("keys=.*&?", "");
                if (queryString.length() > 0 && !queryString.endsWith(String.valueOf('&'))) {
                    queryString += '&';
                }
                // Save the current content so the user doesn't loose his changes and then redirect the request back to
                // the requester, passing the keys to the WYSIWYG_INPUT and WYSIWYG_ERROR maps in the query string.
                queryString += "keys=" + save(mreq, wysiwygNames, errors);
                mreq.sendRedirect(res, referer + '?' + queryString);
            } else {
                chain.doFilter(mreq, res);
            }
        } else {
            chain.doFilter(req, res);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Filter#init(FilterConfig)
     */
    public void init(FilterConfig config) throws ServletException
    {
    }

    /**
     * Saves the current content of the WYSIWYG editors after a conversion failure.
     * 
     * @param mreq The request holding the content to be saved.
     * @param wysiwygNames The request parameters holding the content to be saved.
     * @param errors The conversion exceptions.
     * @return The comma-separated list of keys to {@link #WYSIWYG_INPUT} map.
     */
    private String save(MutableServletRequest mreq, String[] wysiwygNames, Throwable[] errors)
    {
        Map<String, String> wysiwygInput = (Map<String, String>) mreq.getSessionAttribute(WYSIWYG_INPUT);
        if (wysiwygInput == null) {
            wysiwygInput = new HashMap<String, String>();
            mreq.setSessionAttribute(WYSIWYG_INPUT, wysiwygInput);
        }

        Map<String, Throwable> wysiwygError = (Map<String, Throwable>) mreq.getSessionAttribute(WYSIWYG_ERROR);
        if (wysiwygError == null) {
            wysiwygError = new HashMap<String, Throwable>();
            mreq.setSessionAttribute(WYSIWYG_ERROR, wysiwygError);
        }

        StringBuffer keys = new StringBuffer();
        for (int i = 0; i < wysiwygNames.length; i++) {
            String key = RandomStringUtils.randomAlphanumeric(4);
            wysiwygInput.put(key, mreq.getRequest().getParameter(wysiwygNames[i]));
            if (errors[i] != null) {
                wysiwygError.put(key, errors[i]);
            }
            keys.append(key);
            keys.append(i < wysiwygNames.length - 1 ? "," : "");
        }
        return keys.toString();
    }
}
