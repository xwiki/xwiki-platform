package org.xwiki.officeimporter.document;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xwiki.officeimporter.converter.OfficeConverterResult;

public privileged aspect XHTMLOfficeDocumentCompatibilityAspect
{
    @Deprecated
    private Map<String, byte[]> XHTMLOfficeDocument.artifacts;

    /**
     * Creates a new {@link XHTMLOfficeDocument}.
     *
     * @param document the w3c dom representing the office document.
     * @param artifacts artifacts for this office document.
     * @deprecated Since 13.1RC1 use {@link #XHTMLOfficeDocument(Document, Set, OfficeConverterResult)}.
     */
    @Deprecated
    public XHTMLOfficeDocument.new(Document document, Map<String, byte[]> artifacts)
    {
        this(document, Collections.emptySet(), null);
        this.artifacts = artifacts;
    }

    /**
     * Overrides {@link CompatibilityOfficeDocument#getArtifacts()}.
     */
    @Deprecated
    public Map<String, byte[]> XHTMLOfficeDocument.getArtifacts()
    {
        if (this.artifacts == null) {
            this.artifacts = new HashMap<>();
            FileInputStream fis = null;

            for (File file : this.artifactFiles) {
                try {
                    fis = new FileInputStream(file);
                    this.artifacts.put(file.getName(), IOUtils.toByteArray(fis));
                } catch (IOException e) {
                    // FIXME
                    e.printStackTrace();
                } finally {
                    IOUtils.closeQuietly(fis);
                }
            }
        }
        return this.artifacts;
    }
}
