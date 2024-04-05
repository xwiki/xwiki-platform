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
package org.xwiki.rendering.internal.macro;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.w3c.dom.Document;
import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.MacroRequiredRight;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightsAnalyzer;
import org.xwiki.properties.ConverterManager;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;
import org.xwiki.xml.html.filter.HTMLFilter;
import org.xwiki.xml.internal.html.filter.SanitizerDetectorFilter;

/**
 * Required rights analyzer for the HTML macro.
 *
 * @version $Id$
 * @since 15.10
 */
@Component
@Singleton
@Named("html")
public class HTMLMacroRequiredRightsAnalyzer implements MacroRequiredRightsAnalyzer
{
    private static final String TRANSLATION_PREFIX = "rendering.macro.htmlRequiredRights.";

    @Inject
    private HTMLCleaner htmlCleaner;

    @Inject
    private ConverterManager converter;

    @Inject
    @Named(SanitizerDetectorFilter.ID)
    private HTMLFilter restrictedFilterDetector;

    @Override
    public void analyze(MacroBlock macroBlock, MacroRequiredRightReporter reporter)
    {
        boolean wiki = Boolean.TRUE.equals(this.converter.convert(Boolean.class, macroBlock.getParameter("wiki")));
        String cleanParameter = macroBlock.getParameter("clean");
        // Cleaning is enabled by default.
        boolean clean =
            cleanParameter == null || Boolean.TRUE.equals(this.converter.convert(Boolean.class, cleanParameter));

        if (wiki) {
            reporter.analyzeContent(macroBlock, macroBlock.getContent());
        }

        if (!clean) {
            reporter.report(macroBlock, List.of(MacroRequiredRight.SCRIPT),
                TRANSLATION_PREFIX + "noClean");
        } else if (wiki) {
            reporter.report(macroBlock, List.of(MacroRequiredRight.MAYBE_SCRIPT),
                TRANSLATION_PREFIX + "wikiContent");
        } else {
            HTMLCleanerConfiguration cleanerConfiguration = this.htmlCleaner.getDefaultConfiguration();
            Map<String, String> parameters = new HashMap<>(cleanerConfiguration.getParameters());
            // Assume HTML 5 as there is no real way to determine the target version of the rendering action. Also,
            // the analysis shouldn't really be affected by the HTML version.
            parameters.put(HTMLCleanerConfiguration.HTML_VERSION, "5");
            cleanerConfiguration.setParameters(parameters);

            // Add the filter for detecting content that would be filtered by restricted mode.
            List<HTMLFilter> filters = new ArrayList<>(cleanerConfiguration.getFilters());
            filters.add(this.restrictedFilterDetector);
            cleanerConfiguration.setFilters(filters);

            Document document = this.htmlCleaner.clean(new StringReader(macroBlock.getContent()), cleanerConfiguration);
            if (Boolean.parseBoolean(document.getDocumentElement()
                .getAttribute(SanitizerDetectorFilter.ATTRIBUTE_FILTERED)))
            {
                reporter.report(macroBlock, List.of(MacroRequiredRight.SCRIPT),
                    TRANSLATION_PREFIX + "dangerousContent");
            }
        }
    }
}
