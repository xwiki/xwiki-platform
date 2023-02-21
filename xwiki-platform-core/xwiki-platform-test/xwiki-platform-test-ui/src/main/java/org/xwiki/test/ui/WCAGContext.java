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
package org.xwiki.test.ui;

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;
import com.deque.html.axecore.selenium.AxeReporter;

import javax.validation.constraints.Max;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Map.entry;


public class WCAGContext{

  /** List of rules to fail on.
   * Failing rules will fail the test suite, non failing rules will only return a warning if violated.
   * Those rules are identified by ID, and a full list of ids can be found here:
   * <a href="https://github.com/dequelabs/axe-core/blob/develop/doc/rule-descriptions.md#deprecated-rules">...</a>
   * Only rules for validating up to WCAG level AA 2.1 are specified here.
   * Rules not defined here will keep their default behavior and fail the test-suite on violation.
   * There is a parameter for rules *reviewOnFail* in axe-core, however it's not possible to change it using the
   * current selenium integration library.
   **/
  private static final Map<String, Boolean> FAILS_ON_RULE = Map.<String, Boolean>ofEntries(

    // WCAG 2.0 Level A & AA Rules
    entry("area-alt", false),
    entry("aria-allowed-attr", false),
    entry("aria-command-name", false),
    entry("aria-hidden-body", false),
    entry("aria-hidden-focus", false),
    entry("aria-input-field-name", false),
    entry("aria-meter-name", false),
    entry("aria-progressbar-name", false),
    entry("aria-required-attr", false),
    entry("aria-required-children", false),
    entry("aria-required-parent", false),
    entry("aria-roledescription", false),
    entry("aria-roles", false),
    entry("aria-toggle-field-name", false),
    entry("aria-tooltip-name", false),
    entry("aria-valid-attr-value", false),
    entry("aria-valid-attr", false),
    entry("audio-caption", false),
    entry("blink", false),
    entry("button-name", false),
    entry("bypass", false),
    entry("color-contrast", false),
    entry("definition-list", false),
    entry("dlitem", false),
    entry("document-title", false),
    entry("duplicate-id-active", false),
    entry("duplicate-id-aria", false),
    entry("duplicate-id", false),
    entry("form-field-multiple-labels", false),
    entry("frame-focusable-content", false),
    entry("frame-title-unique", false),
    entry("frame-title", false),
    entry("html-has-lang", false),
    entry("html-lang-valid", false),
    entry("html-xml-lang-mismatch", false),
    entry("image-alt", false),
    entry("input-button-name", false),
    entry("input-image-alt", false),
    entry("label", false),
    entry("link-in-text-block", false),
    entry("link-name", false),
    entry("list", false),
    entry("listitem", false),
    entry("marquee", false),
    entry("meta-refresh", false),
    entry("meta-viewport", false),
    entry("nested-interactive", false),
    entry("no-autoplay-audio", false),
    entry("object-alt", false),
    entry("role-img-alt", false),
    entry("scrollable-region-focusable", false),
    entry("select-name", false),
    entry("server-side-image-map", false),
    entry("svg-img-alt", false),
    entry("td-headers-attr", false),
    entry("th-has-data-cells", false),
    entry("valid-lang", false),
    entry("video-caption", false),

    // WCAG 2.1 Level A & AA Rules
    entry("autocomplete-valid", false),
    entry("avoid-inline-spacing", false),

    // WCAG 2.2 Level A & AA Rules
    entry("target-size", false)
  );
  /**
   *
   */
  private static final List<String> VALIDATE_TAGS = Arrays.asList("wcag2a", "wcag2aa", "wcag21a", "wcag21aa");
  private static final List<String> DISABLED_RULES = Arrays.asList();

  private static class WCAGTestResults {
    private String failReport = "";
    private String warnReport = "";
    private WCAGTestResults(XWikiWebDriver driver, Class validationClass, Results axeResults) {
      // Generate the report as soon as the results are in.
      List<Rule> failingViolations = axeResults.getViolations()
        .stream()
        .filter(rule -> !FAILS_ON_RULE.containsKey(rule.getId()) || FAILS_ON_RULE.get(rule.getId()))
        /* If the ruleid is not defined in FAILS_ON_RULE, the default behavior will be to add it to the fails.
        In order to resolve these test-suite fails quickly, set them as "false" in FAILS_ON_RULE.
         */
        .collect(Collectors.toList());
      if(failingViolations!=null && failingViolations.size()!=0) {
        AxeReporter.getReadableAxeResults(String.format("WCAG fails on %s", validationClass.getName()),
          driver, failingViolations);
        this.failReport = AxeReporter.getAxeResultString();
      }
      List<Rule> warningViolations = axeResults.getViolations()
        .stream()
        .filter(rule -> FAILS_ON_RULE.containsKey(rule.getId()) && !FAILS_ON_RULE.get(rule.getId()))
        .collect(Collectors.toList());
      if (warningViolations!=null && warningViolations.size()!=0) {
        AxeReporter.getReadableAxeResults(String.format("WCAG warnings on %s", validationClass.getName()),
          driver, warningViolations);
        this.warnReport = AxeReporter.getAxeResultString();
      }
    }
    public String getFailReport() {
      return this.failReport;
    }
    public String getWarnReport() {
      return this.warnReport;
    }
    protected boolean failIsEmpty() {
      return (this.failReport.equals(""));
    }
    protected boolean warnIsEmpty() {
      return (this.warnReport.equals(""));
    }
  }
  public List<WCAGTestResults> wcagResults = new ArrayList<>();
  public boolean wcagEnabled = true;
  public long wcagTimer = 0;
  private HashMap<String, ArrayList<Class> > wcagValidationCache = new HashMap<>();
  /* List of tags to validate. */


  /**
   * Creates and initialize an axe builder with context options.
   * @return Ready to use AxeBuilder
   */
  public AxeBuilder getAxeBuilder() {
    // Build the query options from context
    AxeBuilder axeBuilder = new AxeBuilder();
    axeBuilder.withTags(VALIDATE_TAGS);
    if(DISABLED_RULES.size()>0) {
      axeBuilder.disableRules(DISABLED_RULES);
    }
    return axeBuilder;
  }

  /**
   * Checks if there's a need to perform an accessibility analysis of a basePage.
   *
   */
  public boolean checkAccessibility(String url, Class pageClass) {
    return this.isWcagEnabled() &&
      (!this.wcagValidationCache.containsKey(url) ||
        !this.wcagValidationCache.get(url).contains(pageClass));
  }

  /**
   * Appends WCAG results to the test suite cache.
   *
   */
  public void addWcagResults(XWikiWebDriver driver, Class pageClass , Results newViolations) {
    String url = driver.getCurrentUrl();
    if (this.checkAccessibility(url,pageClass)) {
      this.wcagResults.add(new WCAGTestResults(driver, pageClass, newViolations));
      this.wcagValidationCache.putIfAbsent(url, new ArrayList<>());
      this.wcagValidationCache.get(url).add(pageClass);
    }
  }
  /**
   * Adds time used for WCAG validation to the test suite total.
   *
   */
  public void addWcagTime(long time)
  {
    this.wcagTimer += time;
  }
  /**
   * Set the WCAG validation enabled setup parameter for the test suite.
   *
   */
  public void setWcagEnabled(boolean wcag)
  {
    this.wcagEnabled = wcag;
  }
  public boolean isWcagEnabled()
  {
    return this.wcagEnabled;
  }
  public boolean hasWCAGFails() {
    for (WCAGTestResults results : this.wcagResults) {
      if(!results.failIsEmpty()) {
        return true;
      }
    }
    return false;
  }
  public boolean hasWCAGWarnings() {
    for (WCAGTestResults results : this.wcagResults) {
      if(!results.warnIsEmpty()) {
        return true;
      }
    }
    return false;
  }
  public String buildFailsReport() {
    StringBuilder mergedReport = new StringBuilder();
    boolean failsEmpty = true;
    for(WCAGTestResults result : this.wcagResults) {
      if(!result.failIsEmpty()) {
        if (failsEmpty) {
          mergedReport.append("WCAG fails:\n");
          failsEmpty = false;
        }
        mergedReport.append(result.getFailReport());
      }
    }
    mergedReport.append("\n");
    return mergedReport.toString();
  }
  public String buildWarningsReport() {
    StringBuilder mergedReport = new StringBuilder();
    boolean warningsEmpty = true;
    for(WCAGTestResults result : this.wcagResults) {
      if(!result.warnIsEmpty()) {
        if (warningsEmpty) {
          mergedReport.append("WCAG warnings:\n");
          warningsEmpty = false;
        }
        mergedReport.append(result.getWarnReport());
      }
    }
    return mergedReport.toString();
  }
  /**
   * Writes a WCAG report to a file for proper accessibility warnings and fails examination.
   * @param outputFile file to write the object to.
   * @param output object to keep in memory.
   */
  public static void writeWCAGReportToFile(final File outputFile, final Object output)
  {
    try (Writer writer =
           new BufferedWriter(
             new OutputStreamWriter(
               new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
      writer.write(output.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}