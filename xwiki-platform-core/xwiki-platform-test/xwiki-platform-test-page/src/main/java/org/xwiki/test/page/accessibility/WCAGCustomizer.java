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

import java.util.ArrayList;
import java.util.List;

import org.xwiki.test.page.PageTest;

/**
 * Configures a WCAG validation performed by {@link PageTest#assertWCAG} (or the default configuration of
 * the automatic validation of {@link WCAG}).
 * <p>
 * The validation runs axe-core in a Node.js/jsdom bridge, with the same rule tags as the WCAG validation of the
 * docker based functional tests ({@code wcag2a}, {@code wcag2aa}, {@code wcag21a}, {@code wcag21aa}: every rule
 * up to WCAG 2.1 level AA). Violations fail the validation, while incomplete checks (which jsdom cannot always
 * resolve) are only logged.
 * <p>
 * Since {@code renderPage} renders a content fragment rather than a full document, and jsdom provides no style
 * or layout engine, a few rules can never be meaningfully satisfied and are disabled by default:
 * <ul>
 *   <li>{@code bypass}: a content fragment has no repeated blocks or landmarks to bypass</li>
 *   <li>{@code color-contrast}: requires the computed CSS of a rendered page</li>
 *   <li>{@code document-title}: a content fragment has no document title</li>
 *   <li>{@code html-has-lang}, {@code html-lang-valid}, {@code html-xml-lang-mismatch}: a content fragment has
 *       no root {@code html} element to carry the language of the page</li>
 *   <li>{@code landmark-one-main}, {@code region}: a content fragment has no landmarks (those rules are also
 *       outside the default tags, they are listed to stay safe if the tags are overridden)</li>
 * </ul>
 * This default disabled list is specific to the page tests and is the documented divergence from the docker
 * based validation, which disables no rule; use {@link #withDisabledRules} to extend it when a test needs to.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public class WCAGCustomizer
{
    /**
     * The axe-core rule tags to take into account.
     */
    // Note: this list needs to be in sync with org.xwiki.test.ui.WCAGContext.VALIDATE_TAGS and with the
    // vitest configuration in @xwiki/platform-test-accessibility (assert-axe.ts).
    private static final List<String> DEFAULT_TAGS = List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa");

    private static final List<String> DEFAULT_DISABLED_RULES = List.of(
        "bypass",
        "color-contrast",
        "document-title",
        "html-has-lang",
        "html-lang-valid",
        "html-xml-lang-mismatch",
        "landmark-one-main",
        "region");

    private final List<String> tags;

    private final List<String> disabledRules;

    private String pageLabel;

    /**
     * Creates a customizer with the default configuration: the docker-aligned tags, and the page-test specific
     * default disabled rules.
     */
    public WCAGCustomizer()
    {
        this.tags = new ArrayList<>(DEFAULT_TAGS);
        this.disabledRules = new ArrayList<>(DEFAULT_DISABLED_RULES);
    }

    /**
     * Disables additional rules, on top of the documented default disabled list.
     *
     * @param rules the ids of the rules to disable (see
     *     <a href="https://github.com/dequelabs/axe-core/blob/develop/doc/rule-descriptions.md">the rule
     *     descriptions</a>)
     * @return this customizer, for chaining
     */
    public WCAGCustomizer withDisabledRules(String... rules)
    {
        this.disabledRules.addAll(List.of(rules));
        return this;
    }

    /**
     * Replaces the rule tags to take into account. Defaults to the tags used by the docker based functional
     * tests; restoring or overriding them changes the set of validated rules.
     *
     * @param tags the axe-core tags of the rules to take into account
     * @return this customizer, for chaining
     */
    public WCAGCustomizer withTags(String... tags)
    {
        this.tags.clear();
        this.tags.addAll(List.of(tags));
        return this;
    }

    /**
     * Sets the label identifying the validated page in the failure report.
     *
     * @param pageLabel the label of the page
     * @return this customizer, for chaining
     */
    public WCAGCustomizer withPageLabel(String pageLabel)
    {
        this.pageLabel = pageLabel;
        return this;
    }

    /**
     * @return the axe-core rule tags of the rules to take into account
     */
    public List<String> getTags()
    {
        return this.tags;
    }

    /**
     * @return the ids of the rules to disable
     */
    public List<String> getDisabledRules()
    {
        return this.disabledRules;
    }

    /**
     * @return the label identifying the validated page in the failure report, or {@code null} when unset
     */
    public String getPageLabel()
    {
        return this.pageLabel;
    }
}
