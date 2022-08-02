package org.xwiki.officeimporter.document;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.officeimporter.converter.OfficeConverterResult;
import org.xwiki.rendering.block.XDOM;

public privileged aspect XDOMOfficeDocumentCompatibilityAspect
{
    @Deprecated
    private Map<String, byte[]> XDOMOfficeDocument.artifacts;

    /**
     * Creates a new {@link XDOMOfficeDocument}.
     *
     * @param xdom {@link XDOM} corresponding to office document content.
     * @param artifacts artifacts for this office document.
     * @param componentManager {@link ComponentManager} used to lookup for various renderers.
     * @deprecated Since 13.1RC1 use {@link #XDOMOfficeDocument(XDOM, Set, ComponentManager, OfficeConverterResult)}.
     */
    @Deprecated
    public XDOMOfficeDocument.new(XDOM xdom, Map<String, byte[]> artifacts, ComponentManager componentManager)
    {
        this(xdom, Collections.emptySet(), componentManager, null);
        this.artifacts = artifacts;
    }

    /**
     * Overrides {@link CompatibilityOfficeDocument#getArtifacts()}.
     */
    @Deprecated
    public Map<String, byte[]> XDOMOfficeDocument.getArtifacts()
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
