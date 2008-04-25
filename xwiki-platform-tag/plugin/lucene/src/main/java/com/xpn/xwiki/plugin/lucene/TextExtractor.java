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
package com.xpn.xwiki.plugin.lucene;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.plugin.lucene.textextraction.MSExcelTextExtractor;
import com.xpn.xwiki.plugin.lucene.textextraction.MSPowerPointTextExtractor;
import com.xpn.xwiki.plugin.lucene.textextraction.MSWordTextExtractor;
import com.xpn.xwiki.plugin.lucene.textextraction.MimetypeTextExtractor;
import com.xpn.xwiki.plugin.lucene.textextraction.PDFTextExtractor;
import com.xpn.xwiki.plugin.lucene.textextraction.PlainTextExtractor;
import com.xpn.xwiki.plugin.lucene.textextraction.XmlTextExtractor;

/**
 * Extraction of text from various binary formats. Extraction itself is done by the textExtractor
 * classes in Packages below <code>org.outerj.daisy</code> taken from the <a
 * href="http://new.cocoondev.org/daisy">Daisy project </a>.
 * 
 * @version $Id: $
 */
public class TextExtractor
{
    private static final Log LOG = LogFactory.getLog(TextExtractor.class);

    static final Map<String, MimetypeTextExtractor> textExtractors = new HashMap<String, MimetypeTextExtractor>();

    static {
        // TODO: make text extractors more pluggable by moving this into a config file.
        final XmlTextExtractor xmlTextExtractor = new XmlTextExtractor();
        textExtractors.put("application/xhtml+xml", xmlTextExtractor);
        textExtractors.put("text/xml", xmlTextExtractor);
        textExtractors.put("text/plain", new PlainTextExtractor());
        textExtractors.put("application/pdf", new PDFTextExtractor());
        // textExtractors.put ("application/vnd.sun.xml.writer", new OpenOfficeTextExtractor ());
        textExtractors.put("application/msword", new MSWordTextExtractor());
        textExtractors.put("application/ms-word", new MSWordTextExtractor());
        textExtractors.put("application/vnd.msword", new MSWordTextExtractor());
        textExtractors.put("application/vnd.ms-word", new MSWordTextExtractor());
        textExtractors.put("application/vnd.ms-powerpoint", new MSPowerPointTextExtractor());
        textExtractors.put("application/ms-powerpoint", new MSPowerPointTextExtractor());
        textExtractors.put("application/ms-excel", new MSExcelTextExtractor());
        textExtractors.put("application/vnd.ms-excel", new MSExcelTextExtractor());
    }

    /**
     * @param content
     * @param mimetype
     * @return
     */
    public static String getText(byte[] content, String mimetype)
    {
        final MimetypeTextExtractor extractor =
            (MimetypeTextExtractor) textExtractors.get(mimetype);
        if (extractor != null) {
            try {
                return extractor.getText(content);
            } catch (Exception e) {
                LOG.error("error getting text for mimetype " + mimetype, e);
                e.printStackTrace();
            }
        } else {
            LOG.info("no text extractor for mimetype " + mimetype);
        }
        return null;
    }
}
