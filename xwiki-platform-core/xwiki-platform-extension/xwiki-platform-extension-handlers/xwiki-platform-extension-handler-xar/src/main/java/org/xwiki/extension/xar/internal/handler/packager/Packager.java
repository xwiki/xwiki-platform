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
package org.xwiki.extension.xar.internal.handler.packager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xwiki.component.annotation.Role;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.wikistream.xar.internal.XarEntry;
import org.xwiki.wikistream.xar.internal.XarException;
import org.xwiki.wikistream.xar.internal.XarFile;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Take care of parsing xar files and handling database actions.
 * 
 * @version $Id$
 * @since 4.0M1
 */
@Role
public interface Packager
{
    void importXAR(File xarFile, PackageConfiguration configuration) throws IOException, XWikiException,
        ComponentLookupException;

    void unimportXAR(File xarFile, PackageConfiguration configuration) throws IOException, XWikiException, XarException;

    void unimportPages(Collection<XarEntry> pages, PackageConfiguration configuration) throws XWikiException;

    /**
     * @since 5.3RC1
     */
    void parseDocument(InputStream in, ContentHandler documentHandler) throws ParserConfigurationException,
        SAXException, IOException, NotADocumentException;

    /**
     * @since 5.3RC1
     */
    XWikiDocument getXWikiDocument(WikiReference wikiReference, LocalDocumentReference documentReference,
        XarFile previousXarFile) throws NotADocumentException, ParserConfigurationException, SAXException, IOException;
}
