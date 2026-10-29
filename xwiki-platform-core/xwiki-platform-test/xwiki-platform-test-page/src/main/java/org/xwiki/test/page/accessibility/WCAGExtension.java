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
package org.xwiki.test.page.accessibility;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.xwiki.test.page.PageTest;

/**
 * The JUnit Jupiter extension behind the {@link WCAG} annotation: collects the pages rendered during a test
 * method for which the validation is active (the method, or its class, is annotated with {@code @WCAG}), then
 * validates them all, deduplicated, at the end of the test. The extension is registered by {@link PageTest} so
 * that every page test can opt in with the annotation alone; it stays inactive without it.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public class WCAGExtension implements BeforeEachCallback, AfterEachCallback
{
    private boolean enabled;

    private String testMethodName;

    private final Map<String, String> renderedPages = new LinkedHashMap<>();

    @Override
    public void beforeEach(ExtensionContext context)
    {
        this.enabled = false;
        this.renderedPages.clear();
        Method testMethod = context.getRequiredTestMethod();
        this.testMethodName = testMethod.getName();
        WCAG methodWCAG = testMethod.getAnnotation(WCAG.class);
        if (methodWCAG != null) {
            this.enabled = methodWCAG.enabled();
        } else {
            WCAG classWCAG = context.getRequiredTestClass().getAnnotation(WCAG.class);
            this.enabled = classWCAG != null && classWCAG.enabled();
        }
    }

    @Override
    public void afterEach(ExtensionContext context)
    {
        try {
            if (this.enabled && !this.renderedPages.isEmpty()) {
                WCAGValidator.validate(this.renderedPages, this.testMethodName);
            }
        } finally {
            this.enabled = false;
            this.renderedPages.clear();
        }
    }

    /**
     * Records a rendered page for the validation at the end of the current test. Rendering the same HTML twice
     * only validates it once, and the first label given to a page wins.
     *
     * @param pageHTML the rendered HTML of the page
     * @param pageLabel the label identifying the page in the report, usually its reference
     */
    public void collect(String pageHTML, String pageLabel)
    {
        if (this.enabled) {
            this.renderedPages.putIfAbsent(pageHTML, pageLabel);
        }
    }
}
