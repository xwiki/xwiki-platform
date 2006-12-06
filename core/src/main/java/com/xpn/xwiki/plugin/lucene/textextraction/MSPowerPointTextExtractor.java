/**
 * Created by IntelliJ IDEA.
 * User: lokesh
 * Date: Oct 17, 2006
 * Time: 3:31:38 PM
 * To change this template use File | Settings | File Templates.
 */

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
