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
package com.xpn.xwiki.internal.filter;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.InputSource;

/**
 * Dispatch event to passed list of filters.
 * 
 * @version $Id$
 */
public class XWikiDocumentFilterCollection implements XWikiDocumentFilter
{
    private final List<? extends XWikiDocumentFilter> filters;

    /**
     * @param filters the filters to send events to
     */
    public XWikiDocumentFilterCollection(List<? extends XWikiDocumentFilter> filters)
    {
        this.filters = filters;
    }

    @Override
    public void beginWiki(String name, FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.beginWiki(name, parameters);
        }
    }

    @Override
    public void endWiki(String name, FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.endWiki(name, parameters);
        }
    }

    @Override
    public void beginWikiSpace(String name, FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.beginWikiSpace(name, parameters);
        }
    }

    @Override
    public void endWikiSpace(String name, FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.endWikiSpace(name, parameters);
        }
    }

    @Override
    public void beginWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.beginWikiDocument(name, parameters);
        }
    }

    @Override
    public void endWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.endWikiDocument(name, parameters);
        }
    }

    @Override
    public void beginWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.beginWikiDocumentLocale(locale, parameters);
        }
    }

    @Override
    public void endWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.endWikiDocumentLocale(locale, parameters);
        }
    }

    @Override
    public void beginWikiDocumentRevision(String revision, FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.beginWikiDocumentRevision(revision, parameters);
        }
    }

    @Override
    public void endWikiDocumentRevision(String revision, FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.endWikiDocumentRevision(revision, parameters);
        }
    }

    @Override
    public void onWikiAttachment(String name, InputStream content, Long size, FilterEventParameters parameters)
        throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.onWikiAttachment(name, content, size, parameters);
        }
    }

    @Override
    public void beginWikiDocumentAttachment(String name, InputSource content, Long size,
        FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.beginWikiDocumentAttachment(name, content, size, parameters);
        }
    }

    @Override
    public void endWikiDocumentAttachment(String name, InputSource content, Long size, FilterEventParameters parameters)
        throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.endWikiDocumentAttachment(name, content, size, parameters);
        }
    }

    @Override
    public void beginWikiAttachmentRevisions(FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.beginWikiAttachmentRevisions(parameters);
        }
    }

    @Override
    public void endWikiAttachmentRevisions(FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.beginWikiAttachmentRevisions(parameters);
        }
    }

    @Override
    public void beginWikiAttachmentRevision(String revision, InputSource content, Long size,
        FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.beginWikiAttachmentRevision(revision, content, size, parameters);
        }
    }

    @Override
    public void endWikiAttachmentRevision(String revision, InputSource content, Long size,
        FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.endWikiAttachmentRevision(revision, content, size, parameters);
        }
    }

    @Override
    public void beginWikiClass(FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.beginWikiClass(parameters);
        }
    }

    @Override
    public void endWikiClass(FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.endWikiClass(parameters);
        }
    }

    @Override
    public void beginWikiClassProperty(String name, String type, FilterEventParameters parameters)
        throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.beginWikiClassProperty(name, type, parameters);
        }
    }

    @Override
    public void endWikiClassProperty(String name, String type, FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.endWikiClassProperty(name, type, parameters);
        }
    }

    @Override
    public void onWikiClassPropertyField(String name, String value, FilterEventParameters parameters)
        throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.onWikiClassPropertyField(name, value, parameters);
        }
    }

    @Override
    public void beginWikiObject(String name, FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.beginWikiObject(name, parameters);
        }
    }

    @Override
    public void endWikiObject(String name, FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.endWikiObject(name, parameters);
        }
    }

    @Override
    public void onWikiObjectProperty(String name, Object value, FilterEventParameters parameters) throws FilterException
    {
        for (XWikiDocumentFilter filter : filters) {
            filter.onWikiObjectProperty(name, value, parameters);
        }
    }
}
