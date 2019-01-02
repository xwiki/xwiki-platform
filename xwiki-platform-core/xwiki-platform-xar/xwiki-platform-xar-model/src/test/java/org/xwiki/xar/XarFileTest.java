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
package org.xwiki.xar;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.test.ExtensionPackager;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.xar.internal.model.XarModel;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate {@link XarFile}.
 * 
 * @version $Id$
 */
public class XarFileTest
{
    private static final LocalDocumentReference A_PAGE = new LocalDocumentReference("a", "page", Locale.ROOT);

    private static final LocalDocumentReference A_EN = new LocalDocumentReference("a", "en", Locale.ENGLISH);

    private static final LocalDocumentReference X_PAGE = new LocalDocumentReference("x", "page", Locale.ROOT);

    private static final LocalDocumentReference X_EN = new LocalDocumentReference("x", "en", Locale.ENGLISH);

    private static final LocalDocumentReference Z_PAGE = new LocalDocumentReference("z", "page", Locale.ROOT);

    private static final LocalDocumentReference Z_EN = new LocalDocumentReference("z", "en", Locale.ENGLISH);

    private ExtensionPackager extensionPackager;

    @BeforeEach
    public void beforeEach() throws IOException
    {
        this.extensionPackager = new ExtensionPackager(null, new File("target/test-" + new Date().getTime()));
        this.extensionPackager.generateExtensions();
    }

    @Test
    public void XarFile() throws XarException, IOException
    {
        try (XarFile file = new XarFile(extensionPackager.getExtensionFile(new ExtensionId("xar1", "1.0")))) {
            assertEquals(6, file.getEntries().size());

            XarPackageTest.assertEqualsEntry(new XarEntry(A_EN, "a/en.xml", XarModel.ACTION_MERGE, "type1"),
                file.getEntry(A_EN));
            XarPackageTest.assertEqualsEntry(new XarEntry(A_PAGE, "a/page.xml", XarModel.ACTION_SKIP, "type0"),
                file.getEntry(A_PAGE));
            XarPackageTest.assertEqualsEntry(new XarEntry(X_EN, "x/en.xml", XarModel.ACTION_MERGE, "type1"),
                file.getEntry(X_EN));
            XarPackageTest.assertEqualsEntry(new XarEntry(X_PAGE, "x/page.xml", XarModel.ACTION_SKIP, "type0"),
                file.getEntry(X_PAGE));
            XarPackageTest.assertEqualsEntry(new XarEntry(Z_EN, "z/en.xml", XarModel.ACTION_SKIP), file.getEntry(Z_EN));
            XarPackageTest.assertEqualsEntry(new XarEntry(Z_PAGE, "z/page.xml", XarModel.ACTION_SKIP),
                file.getEntry(Z_PAGE));

            Iterator<XarEntry> packageFilesIterator = file.getXarPackage().getPackageFiles().iterator();
            XarPackageTest.assertEqualsEntry(new XarEntry(A_EN, "a/en.xml", XarModel.ACTION_MERGE, "type1"),
                packageFilesIterator.next());
            XarPackageTest.assertEqualsEntry(new XarEntry(A_PAGE, "a/page.xml", XarModel.ACTION_SKIP, "type0"),
                packageFilesIterator.next());
            XarPackageTest.assertEqualsEntry(new XarEntry(X_PAGE, "x/page.xml", XarModel.ACTION_SKIP, "type0"),
                packageFilesIterator.next());
            XarPackageTest.assertEqualsEntry(new XarEntry(X_EN, "x/en.xml", XarModel.ACTION_MERGE, "type1"),
                packageFilesIterator.next());
            XarPackageTest.assertEqualsEntry(new XarEntry(Z_EN, "z/en.xml", XarModel.ACTION_SKIP),
                packageFilesIterator.next());
            XarPackageTest.assertEqualsEntry(new XarEntry(Z_PAGE, "z/page.xml", XarModel.ACTION_SKIP),
                packageFilesIterator.next());
        }
    }
}
