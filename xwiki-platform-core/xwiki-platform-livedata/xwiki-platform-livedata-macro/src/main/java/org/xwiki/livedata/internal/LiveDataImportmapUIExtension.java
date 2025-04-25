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
package org.xwiki.livedata.internal;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.webjars.WebJarsUrlFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Produces the import map for Live Data, allowing other modules to import the utility provided by Live Data using
 * JavaScript modules import syntax.
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
@Component
@Singleton
public class LiveDataImportmapUIExtension implements UIExtension
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Inject
    private WebJarsUrlFactory webJarsUrlFactory;

    @Override
    public String getId()
    {
        return "org.xwiki.platform.livedata.importmap";
    }

    @Override
    public String getExtensionPointId()
    {
        return "org.xwiki.platform.importmap";
    }

    @Override
    public Map<String, String> getParameters()
    {
        return Map.of(
            "order", "1000"
        );
    }

    @Override
    public Block execute()
    {
        String liveDataWebjarURL = this.webJarsUrlFactory.url("org.xwiki.platform:xwiki-platform-livedata-webjar",
            "main.es.js");
        String vueWebjarURL = this.webJarsUrlFactory.url("org.webjars.npm:vue", "dist/vue.runtime.esm-browser.js");
        Map<String, Map<String, String>> imports = Map.of(
            "imports", Map.of(
                "xwiki-livedata", liveDataWebjarURL,
                "vue", vueWebjarURL
            ));
        try {
            return new RawBlock(new ObjectMapper().writeValueAsString(imports), Syntax.PLAIN_1_0);
        } catch (JsonProcessingException e) {
            this.logger.warn("Unable to render livedata importmap. Cause: [{}]", getRootCauseMessage(e));
            // Return a minimal valid json to avoid rendering an invalid sourcemap because of a rendering error
            return new RawBlock("{}", Syntax.PLAIN_1_0);
        }
    }
}
