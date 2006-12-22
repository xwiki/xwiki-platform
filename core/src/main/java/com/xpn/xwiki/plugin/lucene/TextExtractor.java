/*
 * 
 * ===================================================================
 *
 * Copyright (c) 2005 Jens Krämer, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created on 25.01.2005
 *
 */

package com.xpn.xwiki.plugin.lucene;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import com.xpn.xwiki.plugin.lucene.textextraction.*;

/**
 * Extraction of text from various binary formats. Extraction itself is done by
 * the textExtractor classes in Packages below <code>org.outerj.daisy</code>
 * taken from the <a href="http://new.cocoondev.org/daisy">Daisy project </a>.
 * @author <a href="mailto:jk@jkraemer.net">Jens Krämer </a>
 */
public class TextExtractor
{
    private static final Logger LOG            = Logger.getLogger (TextExtractor.class);

    static final Map            textExtractors = new HashMap ();
    static
    {
        // TODO: make text extractors more pluggable by moving this into a config file.
        final XmlTextExtractor xmlTextExtractor = new XmlTextExtractor ();
        textExtractors.put ("application/xhtml+xml", xmlTextExtractor);
        textExtractors.put ("text/xml", xmlTextExtractor);
        textExtractors.put ("text/plain", new PlainTextExtractor());
        textExtractors.put ("application/pdf", new PDFTextExtractor());
//        textExtractors.put ("application/vnd.sun.xml.writer", new OpenOfficeTextExtractor ());
        textExtractors.put ("application/msword", new MSWordTextExtractor ());
        textExtractors.put ("application/ms-word", new MSWordTextExtractor ());
        textExtractors.put ("application/vnd.msword", new MSWordTextExtractor ());
        textExtractors.put ("application/vnd.ms-word", new MSWordTextExtractor ());
        textExtractors.put ("application/vnd.ms-powerpoint", new MSPowerPointTextExtractor());
        textExtractors.put ("application/ms-powerpoint", new MSPowerPointTextExtractor());
        textExtractors.put ("application/ms-excel", new MSExcelTextExtractor());
        textExtractors.put ("application/vnd.ms-excel", new MSExcelTextExtractor());
    }

    /**
     * @param content
     * @param mimetype
     * @return
     */
    public static String getText (byte[] content, String mimetype)
    {
        final MimetypeTextExtractor extractor = (MimetypeTextExtractor) textExtractors.get (mimetype);
        if (extractor != null)
        {
            try
            {
                return extractor.getText (content);
            } catch (Exception e)
            {
                LOG.error ("error getting text for mimetype " + mimetype, e);
                e.printStackTrace ();
            }
        } else
        {
            LOG.info ("no text extractor for mimetype " + mimetype);
        }
        return null;
    }

}
