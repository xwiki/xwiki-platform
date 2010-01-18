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
package com.xpn.xwiki.web;

import javax.portlet.*;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;

public class XWikiPortletResponse implements XWikiResponse {
    private PortletResponse response;

    public XWikiPortletResponse(PortletResponse response) {
        this.response = response;
    }

    public PortletResponse getPortletResponse() {
        return response;
    }

    public String getContentType() {
        if (response instanceof RenderResponse)
          return ((RenderResponse)response).getContentType();
        else
          return "";
    }

    public PortletURL createRenderURL() {
        if (response instanceof RenderResponse)
        return ((RenderResponse)response).createRenderURL();
        return null;
    }

    public PortletURL createActionURL() {
        if (response instanceof RenderResponse)
        return ((RenderResponse)response).createActionURL();
        return null;
    }

    public String getNamespace() {
        if (response instanceof RenderResponse)
        return ((RenderResponse)response).getNamespace();
        return "";
    }

    public void setTitle(String s) {
        if (response instanceof RenderResponse)
        ((RenderResponse)response).setTitle(s);
    }

    public void setContentType(String type) {
        if (response instanceof RenderResponse)
        ((RenderResponse)response).setContentType(type);
    }

    public String getCharacterEncoding() {
        if (response instanceof RenderResponse)
        return ((RenderResponse)response).getCharacterEncoding();
        return "";
    }


    public PrintWriter getWriter() throws IOException {
        if (response instanceof RenderResponse)
        return ((RenderResponse)response).getWriter();
        return null;
    }

    public void setCharacterEncoding(String s) {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().setCharacterEncoding(s);
    }

    public Locale getLocale() {
        if (response instanceof RenderResponse)
        return ((RenderResponse)response).getLocale();
        return null;
    }

    public void setBufferSize(int i) {
        if (response instanceof RenderResponse)
        ((RenderResponse)response).setBufferSize(i);
    }

    public int getBufferSize() {
        if (response instanceof RenderResponse)
        return ((RenderResponse)response).getBufferSize();
        return 0;
    }

    public void flushBuffer() throws IOException {
        if (response instanceof RenderResponse)
        ((RenderResponse)response).flushBuffer();
    }

    public void resetBuffer() {
        if (response instanceof RenderResponse)
        ((RenderResponse)response).resetBuffer();
    }

    public boolean isCommitted() {
        if (response instanceof RenderResponse)
        ((RenderResponse)response).isCommitted();
        return false;
    }

    public void reset() {
        if (response instanceof RenderResponse)
          ((RenderResponse)response).reset();
    }

    public OutputStream getPortletOutputStream() throws IOException {
        if (response instanceof RenderResponse)
        return ((RenderResponse)response).getPortletOutputStream();
        return null;
    }

    public void addProperty(String s, String s1) {
        response.addProperty(s, s1);
    }

    public void setProperty(String s, String s1) {
        response.setProperty(s, s1);
    }

    public String encodeURL(String s) {
        return response.encodeURL(s);
    }


    public void setWindowState(WindowState windowState) throws WindowStateException {
        if (response instanceof ActionResponse)
            ((ActionResponse)response).setWindowState(windowState);
    }

    public void setPortletMode(PortletMode portletMode) throws PortletModeException {
        if (response instanceof ActionResponse)
            ((ActionResponse)response).setPortletMode(portletMode);
    }

    public void setRenderParameters(Map map) {
        if (response instanceof ActionResponse)
            ((ActionResponse)response).setRenderParameters(map);
    }

    public void setRenderParameter(String s, String s1) {
        if (response instanceof ActionResponse)
            ((ActionResponse)response).setRenderParameter(s,s1);
    }

    public void setRenderParameter(String s, String[] strings) {
        if (response instanceof ActionResponse)
            ((ActionResponse)response).setRenderParameter(s, strings);
    }

    /*
    *  Servlet functions
    */
    public HttpServletResponse getHttpServletResponse() {
        try {
            return (HttpServletResponse) getPortletResponse();
        } catch (Exception e) {
            return null;
        }
    }

    public ServletOutputStream getOutputStream() throws IOException {
        if (response instanceof HttpServletResponse)
            return getHttpServletResponse().getOutputStream();
        return null;
    }


    public String encodeRedirectURL(String s) {
        if (response instanceof HttpServletResponse)
            return getHttpServletResponse().encodeRedirectURL(s);
        return null;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String encodeUrl(String s) {
        if (response instanceof HttpServletResponse)
            return getHttpServletResponse().encodeUrl(s);
        return null;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String encodeRedirectUrl(String s) {
        if (response instanceof HttpServletResponse)
            return getHttpServletResponse().encodeRedirectUrl(s);
        return null;
    }

    public void sendError(int i, String s) throws IOException {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().sendError(i,s);
    }

    public void sendError(int i) throws IOException {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().sendError(i);
    }

    public void sendRedirect(String redirect) throws IOException {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().sendRedirect(redirect);
    }

    public void addCookie(Cookie cookie) {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().addCookie(cookie);
    }

    /**
     * Remove a cookie.
     *
     * @param request The servlet request needed to find the cookie to remove
     * @param cookieName The name of the cookie that must be removed.
     */
    public void removeCookie(String cookieName, XWikiRequest request)
    {
        if (response instanceof HttpServletResponse) {
            Cookie cookie = request.getCookie(cookieName);
            if (cookie != null) {
                cookie.setMaxAge(0);
                cookie.setPath(cookie.getPath());
                getHttpServletResponse().addCookie(cookie);
            }
        }
    }



    public void setContentLength(int length) {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().setContentLength(length);
    }

    public void setDateHeader(String name, long value) {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().setDateHeader(name, value);
    }

    public void setIntHeader(String name, int value) {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().setIntHeader(name, value);
    }

    public void setHeader(String name, String value) {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().addHeader(name, value);
    }

    public void addHeader(String name, String value) {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().addHeader(name, value);
    }

    public void addDateHeader(String name, long value) {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().addDateHeader(name, value);
    }

    public void addIntHeader(String name, int value) {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().addIntHeader(name, value);
    }

    public void setStatus(int i) {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().setStatus(i);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void setStatus(int i, String s) {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().setStatus(i, s);
    }

    public void setLocale(Locale locale) {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().setLocale(locale);
    }

    public boolean containsHeader(String name) {
        if (response instanceof HttpServletResponse)
            return getHttpServletResponse().containsHeader(name);
        return false;
    }


}

