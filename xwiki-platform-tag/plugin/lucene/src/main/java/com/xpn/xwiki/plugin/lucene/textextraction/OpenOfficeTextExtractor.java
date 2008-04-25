/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xpn.xwiki.plugin.lucene.textextraction;

import java.io.ByteArrayInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;

import com.xpn.xwiki.plugin.lucene.textextraction.xmlutil.XmlEncodingDetector;

/**
 * Extracts all text from an OpenOffice document.
 */
public class OpenOfficeTextExtractor implements MimetypeTextExtractor
{
    private static final String TEXTNAMESPACE = "http://openoffice.org/2000/text";

    public String getText(byte[] data) throws Exception
    {
        /*
         * the byte array we receive here is in fact a ZIP containing the content.xml,
         * styles.xml,meta.xml and META-INF/manifest.xml files. We are only interested in the
         * content.xml because that's the file containing the actual content (duh)
         */

        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ZipInputStream zis = new ZipInputStream(bis);

        ZipEntry ze = null;
        String zipEntryName = null;
        StringBuffer text = new StringBuffer();

        while ((ze = zis.getNextEntry()) != null
            && !(zipEntryName = ze.getName()).equals("content.xml")) {
        }

        if (zipEntryName != null && zipEntryName.equals("content.xml")) {
            /*
             * we found the correct zip entry. This means the "read pointer" of the zipinputstream
             * points correctly to the beginning of this zip entry and we can pass it to the xml
             * parser like this (will return -1 as soon as the end of the zip entry is reached)
             */

            /*
             * We are using this XmlPullParser because it was impossible to work with a sax parser.
             * The sax parser always wanted to have access to the openoffice dtd. Even tried to
             * write our own entityresolver to work around this problem but didnt work out. In order
             * not to pin ourselves down to a specific sax implementor (where we eg. would be able
             * to specify that we explicitly don't want any check at all against a dtd) we choose
             * not to use sax at all and use a very lightweight type of parsing for this specific
             * goal.
             */

            XmlPullParser parser = new MXParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(zis, XmlEncodingDetector.detectEncoding(data));
            boolean inText = false;

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("p")
                        && parser.getNamespace().equals(TEXTNAMESPACE)) {
                        text.append(' ');
                        inText = true;
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("p")
                        && parser.getNamespace().equals(TEXTNAMESPACE)) {
                        inText = false;
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    if (inText) {
                        String gotText = parser.getText();
                        text.append(gotText);
                    }
                }
            }

        } else {
            throw new Exception("Invalid OpenOffice document format (content.xml not found)");
        }

        return text.toString();
    }
}
