/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 * @author sdumitriu
 */

package com.xpn.xwiki.web;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;

import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.PortletURL;
import javax.portlet.WindowState;
import javax.portlet.WindowStateException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class XWikiServletResponse implements XWikiResponse {
    private HttpServletResponse response;

    public XWikiServletResponse(HttpServletResponse response) {
        this.response = response;
    }

    public HttpServletResponse getHttpServletResponse () {
        return response;
    }


    public void sendRedirect(String redirect) throws IOException {
        response.sendRedirect(redirect);
    }

    public void setContentType(String type) {
        response.setContentType(type);
    }

    public void setBufferSize(int i) {
        response.setBufferSize(i);
    }

    public int getBufferSize() {
        return response.getBufferSize();
    }

    public void flushBuffer() throws IOException {
        response.flushBuffer();
    }

    public void resetBuffer() {
        response.resetBuffer();
    }

    public boolean isCommitted() {
        return response.isCommitted();
    }

    public void reset() {
        response.reset();
    }

    public void setContentLength(int length) {
        response.setContentLength(length);
    }

    public String getCharacterEncoding() {
        return response.getCharacterEncoding();
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    public PrintWriter getWriter() throws IOException {
        return response.getWriter();
    }

    public void setCharacterEncoding(String s) {
        response.setCharacterEncoding(s);
    }

    public void addCookie(Cookie cookie) {
        response.addCookie(cookie);
    }

    public void setLocale(Locale locale) {
        response.setLocale(locale);
    }

    public Locale getLocale() {
        return response.getLocale();
    }

    public void setDateHeader(String name, long value) {
        response.setDateHeader(name, value);
    }

    public void setIntHeader(String name, int value) {
        response.setIntHeader(name, value);
    }

    public void setHeader(String name, String value) {
        response.addHeader(name, value);
    }

    public void addHeader(String name, String value) {
        response.addHeader(name, value);
    }

    public void addDateHeader(String name, long value) {
        response.addDateHeader(name, value);
    }

    public void addIntHeader(String name, int value) {
        response.addIntHeader(name, value);
    }

    public void setStatus(int i) {
        response.setStatus(i);
    }

    /**
     * @deprecated
     */
    public void setStatus(int i, String s) {
        response.setStatus(i, s);
    }

    public boolean containsHeader(String name) {
        return response.containsHeader(name);
    }

    public String encodeURL(String s) {
        return response.encodeURL(s);
    }

    public String encodeRedirectURL(String s) {
        return response.encodeRedirectURL(s);
    }

    /**
     * @deprecated
     */
    public String encodeUrl(String s) {
        return response.encodeUrl(s);
    }

    /**
     * @deprecated
     */
    public String encodeRedirectUrl(String s) {
        return response.encodeRedirectUrl(s);
    }

    public void sendError(int i, String s) throws IOException {
        response.sendError(i, s);
    }

    public void sendError(int i) throws IOException {
        response.sendError(i);
    }

    /*
    *  Portlet Functions
    */
    public void addProperty(String s, String s1) {
    }

    public void setProperty(String s, String s1) {
    }

    public String getContentType() {
        return null;
    }

    public OutputStream getPortletOutputStream() throws IOException {
        return null;
    }

    public PortletURL createRenderURL() {
        return null;
    }

    public PortletURL createActionURL() {
        return null;
    }

    public String getNamespace() {
        return null;
    }

    public void setTitle(String s) {
    }

    public void setWindowState(WindowState windowState) throws WindowStateException {
    }

    public void setPortletMode(PortletMode portletMode) throws PortletModeException {
    }

    public void setRenderParameters(Map map) {
    }

    public void setRenderParameter(String s, String s1) {
    }

    public void setRenderParameter(String s, String[] strings) {
    }

}
