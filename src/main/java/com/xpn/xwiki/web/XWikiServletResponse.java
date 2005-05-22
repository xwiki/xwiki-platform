/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 19 mai 2004
 * Time: 14:28:09
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
