package org.xwiki.wikistream.databaseold.internal.output;

import java.util.Locale;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.databaseold.internal.DatabaseFilter;
import org.xwiki.wikistream.internal.output.AbstractBeanOutputWikiStream;

public class DatabaseOutputWikiStream extends AbstractBeanOutputWikiStream<DatabaseOutputProperties> implements
    DatabaseFilter
{
    public DatabaseOutputWikiStream(DatabaseOutputProperties properties)
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
