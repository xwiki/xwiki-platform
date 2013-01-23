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
package org.xwiki.localization.jar.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.LocalizationException;
import org.xwiki.localization.internal.AbstractURLResourceTranslationBundle;
import org.xwiki.localization.message.TranslationMessageParser;

/**
 * JAR file based implementation of Bundle.
 * 
 * @see AbstractURLResourceTranslationBundle
 * @version $Id$
 * @since 4.5M1
 */
public class JARFileTranslationBundle extends AbstractURLResourceTranslationBundle
{
    /**
     * Location in the translation bundle default language.
     */
    public static final String TRANSLATION = "ApplicationResources.properties";

    public static final Pattern TRANSLATION_PATTERN = Pattern.compile("ApplicationResources([a-zA-Z]{2,8}"
        + "(_([a-zA-Z]{2} | [0-9]{3})" + "(_([0-9][0-9a-zA-Z]{3} | [0-9a-zA-Z]{5,8}))?)?)?.properties");

    /**
     * @param jarFile the jar
     * @param componentManager used to lookup components needed to manipulate wiki documents
     * @param translationMessageParser the parser to use for each message
     * @throws IOException failed to parse passed JAR file
     * @throws LocalizationException failed to find translation resource in the passed JAR file
     */
    public JARFileTranslationBundle(File jarFile, ComponentManager componentManager,
        TranslationMessageParser translationMessageParser) throws ComponentLookupException, IOException,
        LocalizationException
    {
        super(getURL(jarFile), componentManager, translationMessageParser);
    }

    /**
     * @param jarURL the jar URL
     * @param componentManager used to lookup components needed to manipulate wiki documents
     * @param translationMessageParser the parser to use for each message
     * @throws IOException failed to parse passed JAR file
     * @throws LocalizationException failed to find translation resource in the passed JAR file
     */
    public JARFileTranslationBundle(URL jarURL, ComponentManager componentManager,
        TranslationMessageParser translationMessageParser) throws ComponentLookupException, IOException,
        LocalizationException
    {
        super(getURL(jarURL), componentManager, translationMessageParser);
    }

    private static URL getURL(File jarFile) throws IOException, LocalizationException
    {
        URL jarURL = JARUtils.toJARURL(jarFile);

        return getURL(jarURL);
    }

    private static URL getURL(URL jarURL) throws IOException, LocalizationException
    {
        InputStream is = jarURL.openStream();

        try {
            ZipInputStream zis = new ZipInputStream(is);

            for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                if (TRANSLATION_PATTERN.matcher(entry.getName()).matches()) {
                    return new URL(jarURL.toExternalForm() + entry.getName());
                }
            }
        } finally {
            IOUtils.closeQuietly(is);
        }

        throw new LocalizationException("Can't find any translation resource in jar [" + jarURL + "]");
    }
}
