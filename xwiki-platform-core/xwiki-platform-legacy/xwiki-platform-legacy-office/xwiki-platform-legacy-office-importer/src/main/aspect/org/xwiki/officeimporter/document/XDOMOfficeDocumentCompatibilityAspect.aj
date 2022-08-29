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
