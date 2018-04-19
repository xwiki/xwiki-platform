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

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import org.junit.Test;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.xar.internal.model.XarModel;

import static org.junit.Assert.assertEquals;

public class XarPackageTest
{
    private static final LocalDocumentReference SPACE_PAGE = new LocalDocumentReference("Space", "Page", Locale.ROOT);

    private static final LocalDocumentReference SPACE_TRANSLATIONS =
        new LocalDocumentReference("Space", "Translations", Locale.ROOT);

    private static final LocalDocumentReference SPACE_TRANSLATIONS_FR =
        new LocalDocumentReference(SPACE_TRANSLATIONS, Locale.FRENCH);

    private static final LocalDocumentReference SPACE_SKIPPEDPAGE =
        new LocalDocumentReference("Space", "SkippedPage", Locale.ROOT);

    private static final LocalDocumentReference SPACE_SKIPPEDTRANSLATIONS =
        new LocalDocumentReference("Space", "SkippedTranslations", Locale.ROOT);

    private static final LocalDocumentReference SPACE_SKIPPEDTRANSLATIONS_FR =
        new LocalDocumentReference(SPACE_SKIPPEDTRANSLATIONS, Locale.FRENCH);

    private static final LocalDocumentReference SPACE_MERGEDPAGE =
        new LocalDocumentReference("Space", "MergedPage", Locale.ROOT);

    private static final LocalDocumentReference SPACE_MERGEDTRANSLATIONS =
        new LocalDocumentReference("Space", "MergedTranslations", Locale.ROOT);

    private static final LocalDocumentReference SPACE_MERGEDTRANSLATIONS_FR =
        new LocalDocumentReference(SPACE_MERGEDTRANSLATIONS, Locale.FRENCH);

    @Test
    public void readDescriptor() throws Exception
    {
        XarPackage xarPackage = new XarPackage();

        xarPackage.readDescriptor(getClass().getResourceAsStream("/package.xml"));

        assertEquals("author", xarPackage.getPackageAuthor());
        assertEquals("package description", xarPackage.getPackageDescription());
        assertEquals("extension:id", xarPackage.getPackageExtensionId());
        assertEquals("license", xarPackage.getPackageLicense());
        assertEquals("package name", xarPackage.getPackageName());
        assertEquals("1.0", xarPackage.getPackageVersion());

        Collection<XarEntry> files = xarPackage.getPackageFiles();

        assertEquals(9, files.size());

        Iterator<XarEntry> iterator = files.iterator();

        assertEqualsEntry(new XarEntry(SPACE_PAGE, XarModel.ACTION_OVERWRITE, "type0"), iterator.next());
        assertEqualsEntry(new XarEntry(SPACE_TRANSLATIONS, XarModel.ACTION_OVERWRITE, "type1"), iterator.next());
        assertEqualsEntry(new XarEntry(SPACE_TRANSLATIONS_FR, XarModel.ACTION_OVERWRITE, "type2"), iterator.next());
        assertEqualsEntry(new XarEntry(SPACE_SKIPPEDPAGE, XarModel.ACTION_SKIP), iterator.next());
        assertEqualsEntry(new XarEntry(SPACE_MERGEDPAGE, XarModel.ACTION_MERGE), iterator.next());
        assertEqualsEntry(new XarEntry(SPACE_SKIPPEDTRANSLATIONS, XarModel.ACTION_SKIP), iterator.next());
        assertEqualsEntry(new XarEntry(SPACE_SKIPPEDTRANSLATIONS_FR, XarModel.ACTION_SKIP), iterator.next());
        assertEqualsEntry(new XarEntry(SPACE_MERGEDTRANSLATIONS, XarModel.ACTION_MERGE), iterator.next());
        assertEqualsEntry(new XarEntry(SPACE_MERGEDTRANSLATIONS_FR, XarModel.ACTION_MERGE), iterator.next());
    }

    @Test
    public void readDescriptorWithEmptyId() throws Exception
    {
        XarPackage xarPackage = new XarPackage();

        xarPackage.readDescriptor(getClass().getResourceAsStream("/packagewithemptyid.xml"));

        assertEquals(null, xarPackage.getPackageExtensionId());
    }

    private void assertEqualsEntry(XarEntry expected, XarEntry actual)
    {
        assertEquals(expected, actual);
        assertEquals(expected.getEntryName(), actual.getEntryName());
        assertEquals(expected.getDefaultAction(), actual.getDefaultAction());
        assertEquals(expected.getEntryType(), actual.getEntryType());
    }
}
