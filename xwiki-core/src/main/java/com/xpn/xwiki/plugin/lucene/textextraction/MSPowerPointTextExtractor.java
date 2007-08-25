package com.xpn.xwiki.plugin.lucene.textextraction;
import org.apache.poi.hslf.extractor.PowerPointExtractor;

import java.io.ByteArrayInputStream;

/**
 * Text extractor for Microsoft Power Point files.
 */
public class MSPowerPointTextExtractor implements MimetypeTextExtractor {

    public String getText(byte[] data) throws Exception {
        PowerPointExtractor ppe = new PowerPointExtractor(new ByteArrayInputStream(data));
        return ppe.getText(true, true);
    }
}
