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
package com.xpn.xwiki.plugin.lucene.textextraction.xmlutil;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility code to detect the encoding of XML provided as a byte array. This code is based on the
 * class com.sun.syndication.io.XmlReader from the Rome project (https://rome.dev.java.net/), which
 * is licensed under the Apache V2 license (and doesn't include a NOTICE file).
 */
public class XmlEncodingDetector
{
    private static final String UTF_8 = "UTF-8";

    private static final String UTF_16BE = "UTF-16BE";

    private static final String UTF_16LE = "UTF-16LE";

    private static final String UTF_16 = "UTF-16";

    public static String detectEncoding(byte[] data)
    {
        String bomEnc = getBOMEncoding(data);
        String xmlGuessEnc = getXMLGuessEncoding(data);
        String xmlEnc = getXMLPrologEncoding(data, xmlGuessEnc);
        String encoding = calculateRawEncoding(bomEnc, xmlGuessEnc, xmlEnc);
        return encoding;
    }

    // returns the BOM in the stream, NULL if not present,
    // if there was BOM the in the stream it is consumed
    private static String getBOMEncoding(byte[] bytes)
    {
        String encoding = null;

        if (bytes[0] == 0xFE && bytes[1] == 0xFF) {
            encoding = UTF_16BE;
        } else if (bytes[0] == 0xFF && bytes[1] == 0xFE) {
            encoding = UTF_16LE;
        } else if (bytes[0] == 0xEF && bytes[1] == 0xBB && bytes[2] == 0xBF) {
            encoding = UTF_8;
        }
        return encoding;
    }

    // returns the best guess for the encoding by looking the first bytes of the stream, '<?'
    private static String getXMLGuessEncoding(byte[] bytes)
    {
        String encoding = null;

        if (bytes[0] == 0x00 && bytes[1] == 0x3C && bytes[2] == 0x00 && bytes[3] == 0x3F) {
            encoding = UTF_16BE;
        } else if (bytes[0] == 0x3C && bytes[1] == 0x00 && bytes[2] == 0x3F && bytes[3] == 0x00) {
            encoding = UTF_16LE;
        } else if (bytes[0] == 0x3C && bytes[1] == 0x3F && bytes[2] == 0x78 && bytes[3] == 0x6D) {
            encoding = UTF_8;
        }
        return encoding;
    }

    private static final Pattern ENCODING_PATTERN =
        Pattern.compile("^<\\?xml.*encoding=\"(.*)\".*\\?>");

    // returns the encoding declared in the <?xml encoding=...?>, NULL if none
    private static String getXMLPrologEncoding(byte[] data, String guessedEnc)
    {
        String encoding = null;
        if (guessedEnc != null) {
            if (data.length > -1) {
                int endFirstLinePos = Math.min(data.length, 1024);
                for (int i = 0; i < 1024 && i < data.length; i++) {
                    if (data[i] == '\n' || data[i] == '\r') {
                        endFirstLinePos = i;
                        break;
                    }
                }
                String prolog = null;
                try {
                    prolog = new String(data, 0, endFirstLinePos, guessedEnc);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                Matcher m = ENCODING_PATTERN.matcher(prolog);
                encoding = (m.find()) ? m.group(1).toUpperCase() : null;
            }
        }
        return encoding;
    }

    private static String calculateRawEncoding(String bomEnc, String xmlGuessEnc, String xmlEnc)
    {
        String encoding;
        if (bomEnc == null) {
            if (xmlGuessEnc == null || xmlEnc == null) {
                encoding = UTF_8;
            } else if (xmlEnc.equals(UTF_16)
                && (xmlGuessEnc.equals(UTF_16BE) || xmlGuessEnc.equals(UTF_16LE))) {
                encoding = xmlGuessEnc;
            } else {
                encoding = xmlEnc;
            }
        } else if (bomEnc.equals(UTF_8)) {
            if (xmlGuessEnc != null && !xmlGuessEnc.equals(UTF_8)) {
                throw new RuntimeException(RAW_EX_1.format(new Object[] {bomEnc, xmlGuessEnc,
                xmlEnc}));
            }
            if (xmlEnc != null && !xmlEnc.equals(UTF_8)) {
                throw new RuntimeException(RAW_EX_1.format(new Object[] {bomEnc, xmlGuessEnc,
                xmlEnc}));
            }
            encoding = UTF_8;
        } else if (bomEnc.equals(UTF_16BE) || bomEnc.equals(UTF_16LE)) {
            if (xmlGuessEnc != null && !xmlGuessEnc.equals(bomEnc)) {
                throw new RuntimeException(RAW_EX_1.format(new Object[] {bomEnc, xmlGuessEnc,
                xmlEnc}));
            }
            if (xmlEnc != null && !xmlEnc.equals(UTF_16) && !xmlEnc.equals(bomEnc)) {
                throw new RuntimeException(RAW_EX_1.format(new Object[] {bomEnc, xmlGuessEnc,
                xmlEnc}));
            }
            encoding = bomEnc;
        } else {
            throw new RuntimeException(RAW_EX_2
                .format(new Object[] {bomEnc, xmlGuessEnc, xmlEnc}));
        }
        return encoding;
    }

    private static final MessageFormat RAW_EX_1 =
        new MessageFormat("Invalid encoding, BOM [{0}] XML guess [{1}] XML prolog [{2}] encoding mismatch");

    private static final MessageFormat RAW_EX_2 =
        new MessageFormat("Invalid encoding, BOM [{0}] XML guess [{1}] XML prolog [{2}] unknown BOM");
}
