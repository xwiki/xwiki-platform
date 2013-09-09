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
package org.xwiki.wikistream.instance.internal.output;

import java.util.Locale;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.instance.internal.DatabaseFilter;
import org.xwiki.wikistream.internal.output.AbstractBeanOutputWikiStream;

/**
 * 
 * @version $Id$
 * @since 5.2M2
 */
public class InstanceOutputWikiStream extends AbstractBeanOutputWikiStream<InstanceOutputProperties> implements
    DatabaseFilter
{
    public InstanceOutputWikiStream(InstanceOutputProperties properties)
    {
        super(properties);
    }

    // events

    @Override
    public void beginWikiFarm(FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void endWikiFarm(FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void beginWiki(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void endWiki(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void beginWikiSpace(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void endWikiSpace(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void beginWikiDocument(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void endWikiDocument(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void beginWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void endWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void beginWikiDocumentRevision(String version, FilterEventParameters parameters)
        throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void endWikiDocumentRevision(String version, FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void beginWikiAttachment(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void beginWikiAttachmentRevision(String version, FilterEventParameters parameters)
        throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void endWikiAttachmentRevision(String version, FilterEventParameters parameters)
        throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void endWikiAttachment(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void beginWikiClass(FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void endWikiClass(FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void beginWikiClassProperty(String name, String type, FilterEventParameters parameters)
        throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void endWikiClassProperty(String name, String type, FilterEventParameters parameters)
        throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void beginWikiObject(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void endWikiObject(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void beginWikiObjectProperty(String propertyName, String value, FilterEventParameters parameters)
        throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void endWikiObjectProperty(String propertyName, String value, FilterEventParameters parameters)
        throws WikiStreamException
    {
        // TODO Auto-generated method stub

    }
}
