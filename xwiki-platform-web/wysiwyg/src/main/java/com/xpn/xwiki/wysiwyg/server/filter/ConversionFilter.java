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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.wysiwyg.server.cleaner.XHTMLCleaner;
import com.xpn.xwiki.wysiwyg.server.converter.XHTMLConverter;

/**
 * This filter is used to convert the values of request parameters that hold WYSIWYG output from HTML to XWiki syntax.
 * This is needed because the action processing the request expects XWiki syntax and not HTML code in these request
 * parameters. The conversion is done using the new rendering module. It has to be done on the server and not on the
 * client, like the old WYSIWYG editor does. Doing the conversion on the client side by making an asynchronous request
 * to the server is error-prone for the following reason: the WYSIWYG behaves like a text area that can be put anywhere
 * in an HTML page, inside or outside an HTML form; because of this the editor is not aware of what submit buttons are
 * present on the container page and what submit logic these buttons might have associated with them.
 */
public class ConversionFilter implements Filter
{
    private static final Log LOG = LogFactory.getLog(ConversionFilter.class);

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
        String[] xRichTextAreas = req.getParameterValues("xRichTextAreas");
        if (xRichTextAreas != null) {
            XHTMLCleaner cleaner = (XHTMLCleaner) Utils.getComponent(XHTMLCleaner.ROLE, "vnikic");
            XHTMLConverter converter = (XHTMLConverter) Utils.getComponent(XHTMLConverter.ROLE, "xwiki/2.0");
            MutableServletRequestFactory mreqFactory =
                (MutableServletRequestFactory) Utils.getComponent(MutableServletRequestFactory.ROLE, req.getProtocol());
            MutableServletRequest mreq = mreqFactory.newInstance(req);
            for (int i = 0; i < xRichTextAreas.length; i++) {
                String xRichTextArea = xRichTextAreas[i];
                if (StringUtils.isEmpty(xRichTextArea)) {
                    continue;
                }
                String oldValue = req.getParameter(xRichTextArea);
                String newValue = oldValue;
                try {
                    newValue = converter.fromXHTML(cleaner.clean(oldValue));
                } catch (Throwable t) {
                    LOG.error(t.getMessage(), t);
                }
                mreq.setParameter(xRichTextArea, newValue);
            }
            req = mreq;
        }
        chain.doFilter(req, res);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Filter#init(FilterConfig)
     */
    public void init(FilterConfig config) throws ServletException
    {
    }
}
