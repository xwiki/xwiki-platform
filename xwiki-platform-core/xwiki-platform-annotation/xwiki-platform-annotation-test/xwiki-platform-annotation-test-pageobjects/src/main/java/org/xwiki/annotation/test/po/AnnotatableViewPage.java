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
package org.xwiki.annotation.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * @version $Id$
 * @since 4.2M1
 */
public class AnnotatableViewPage extends BaseElement
{
    private static final String XWIKI_ANNOTATION_ADD_SUCCESS = "Annotation added";

    private static final String XWIKI_ANNOTATION_DELETE_SUCCESS = "Annotation deleted";

    private static final String XWIKI_SYNTAX_1_WARNING =
        "Annotations are not available for pages written in XWiki/1.0 syntax.";

    private AnnotationsPane annotationsPane;

    private AnnotationsWindow annotationsWindow;

    private AnnotationsLabel annotationsLabel;

    private StringBuilder script;

    private ViewPage viewPage;

    public AnnotatableViewPage(ViewPage viewPage)
    {
        this.viewPage = viewPage;

        /**
         * Injection of this javascript function is used because the Selenium 2 API does not yet fully support advanced
         * interactions with the mouse. This can't be yet achieved via Selenium 2 API. This function selects the text
         * from the page (drag-over with mouse simulation)
         */
        script = new StringBuilder();
        script.append("function findString (str) {\n");
        script.append("  var strFound;\n");
        script.append("  if (window.find) {\n");
        script.append("    if (parseInt(navigator.appVersion)<4) return;\n");
        script.append("    // CODE FOR BROWSERS THAT SUPPORT window.find\n");
        script.append("    strFound = self.find(str);\n");
        script.append("    if (strFound && self.getSelection && !self.getSelection().anchorNode) {\n");
        script.append("      strFound = self.find(str);\n");
        script.append("    }\n");
        script.append("    if (!strFound) {\n");
        script.append("      strFound = self.find(str,0,1);\n");
        script.append("      while (self.find(str,0,1)) continue;\n");
        script.append("    }\n");
        script.append("  } else if (navigator.appName.indexOf(\"Microsoft\")!=-1) {\n");
        script.append("    // EXPLORER-SPECIFIC CODE\n");
        script.append("    if (TRange != null) {\n");
        script.append("      TRange.collapse(false);\n");
        script.append("      strFound = TRange.findText(str);\n");
        script.append("      if (strFound) TRange.select();\n");
        script.append("    }\n");
        script.append("    if (TRange == null || strFound == 0) {\n");
        script.append("      TRange = self.document.body.createTextRange();\n");
        script.append("      strFound = TRange.findText(str);\n");
        script.append("      if (strFound) TRange.select();\n");
        script.append("    }\n");
        script.append("  } else if (navigator.appName == \"Opera\") {\n");
        script.append("    alert ('Opera browsers not supported, sorry...');\n");
        script.append("    return;\n");
        script.append("  }\n");
        script.append("  if (!strFound) \n");
        script.append("    return;\n");
        script.append("}\n");
        ((JavascriptExecutor) getDriver()).executeScript(script.toString());

        annotationsPane = new AnnotationsPane();
        annotationsWindow = new AnnotationsWindow();
        annotationsLabel = new AnnotationsLabel();
    }

    public ViewPage getWrappedViewPage()
    {
        return this.viewPage;
    }

    public void addAnnotation(String annotatedText, String annotationText)
    {
        addAnnotation(annotatedText, annotationText, true);
    }

    public void addAnnotation(String annotatedText, String annotationText, boolean wait)
    {
        beginAddAnnotation(annotatedText).addAnnotation(annotationText);

        if (wait) {
            // check is the saved successfully message is displayed
            waitForNotificationSuccessMessage(XWIKI_ANNOTATION_ADD_SUCCESS);
        }
    }

    /**
     * Selects the specified text and opens the create annotation dialog.
     * 
     * @param annotatedText the text to select and annotate
     * @return the annotation creation dialog
     * @since 12.10.9
     * @since 13.4.1
     * @since 13.5RC1
     */
    public AnnotationsWindow beginAddAnnotation(String annotatedText)
    {
        selectText(annotatedText);
        simulateCTRL_M();
        annotationsWindow.waitUntilReady();
        return annotationsWindow;
    }

    public void deleteAnnotationByID(String id)
    {
        annotationsLabel.deleteAnnotationById(id);
        waitForNotificationSuccessMessage(XWIKI_ANNOTATION_DELETE_SUCCESS);
    }

    public void deleteAnnotationByText(String annotatedText)
    {
        deleteAnnotationByID(this.annotationsLabel.getAnnotationIdByText(annotatedText));
    }

    public String getAnnotationContentByText(String searchText)
    {
        return annotationsLabel.getAnnotationContentByText(searchText);
    }

    /**
     * @param searchText the text selected by the annotation.
     * @return the annotation id
     * @since 10.6RC1
     * @since 9.11.9
     */
    public String getAnnotationIdByText(String searchText)
    {
        return annotationsLabel.getAnnotationIdByText(searchText);
    }

    // Shows the annotations pane from the top of the page
    public AnnotationsPane showAnnotationsPane()
    {
        return showAnnotationsPane(true);
    }

    /**
     * Open the annotation settings pane using the menu and maybe wait for it.
     * 
     * @param wait whether to wait for the pane to be visible or not
     * @return the annotation settings pane
     * @since 12.10.9
     * @since 13.4.1
     * @since 13.5RC1
     */
    public AnnotationsPane showAnnotationsPane(boolean wait)
    {
        this.annotationsPane.showAnnotationsPane(wait);
        return this.annotationsPane;
    }

    // Hides the annotations pane from the top of the page
    public void hideAnnotationsPane()
    {
        annotationsPane.hideAnnotationsPane();
    }

    // Checks the "Show Annotations" check box.
    public void clickShowAnnotations()
    {
        clickShowAnnotations(false);
    }

    /**
     * Clicks on the checkbox to show the annotations and maybe waits for the annotations to be displayed.
     * 
     * @param wait whether to wait or not for the annotations to be displayed; pass {@code true} only if you know there
     *            are existing annotations to display
     * @since 12.10.9
     * @since 13.4.1
     * @since 13.5RC1
     */
    public void clickShowAnnotations(boolean wait)
    {
        annotationsPane.clickShowAnnotations();
        if (wait) {
            waitForAnnotationsDisplayed();
        }
    }

    /**
     * Wait for at least one annotation to be displayed.
     * 
     * @since 12.10.9
     * @since 13.4.1
     * @since 13.5RC1
     */
    public void waitForAnnotationsDisplayed()
    {
        getDriver().waitUntilElementIsVisible(By.className("annotation-marker"));
    }

    // Un-checks the "Show Annotations" check box.
    public void clickHideAnnotations()
    {
        annotationsPane.clickHideAnnotations();
    }

    /**
     * @return the number of annotations displayed on the page
     * @since 12.10.9
     * @since 13.4.1
     * @since 13.5RC1
     */
    public int getAnnotationCount()
    {
        return getDriver().findElementsWithoutWaiting(By.className("annotation-marker")).size();
    }

    /**
     * Toggle the display of the annotations using the keyboard shortcut.
     * 
     * @param wait whether to wait for the annotations to be displayed or not (as they may need to be fetched from the
     *            server); pass {@code true} only if you know that there are existing annotations
     * @since 12.10.9
     * @since 13.4.1
     * @since 13.5RC1
     */
    public void toggleAnnotationDisplayUsingShortcutKey(boolean wait)
    {
        getDriver().findElement(By.id("body")).sendKeys(Keys.chord(Keys.ALT, "a"));
        if (wait) {
            waitForAnnotationsDisplayed();
        }
    }

    // Checks if the checkBox within AnnotationsPane is visible
    public boolean checkIfClickbuttonExists()
    {
        return annotationsPane.checkIfShowAnnotationsCheckboxExists();
    }

    public void simulateCTRL_M()
    {
        WebElement body = getDriver().findElement(By.id("body"));
        String os = System.getProperty("os.name");
        if (os.equals("Mac OS X")) {
            body.sendKeys(Keys.chord(Keys.COMMAND, "m"));
        } else {
            body.sendKeys(Keys.chord(Keys.CONTROL, "m"));
        }
    }

    /**
     * @param annotationWord string that will be selected on the screen
     */
    public void selectText(String annotationWord)
    {
        ((JavascriptExecutor) getDriver()).executeScript(script + "findString('" + annotationWord + "');");
    }

    public boolean checkIfAnnotationsAreDisabled()
    {
        if (getDriver().findElementsWithoutWaiting(By.id("annotationsdisplay")).size() > 0) {
            return false;
        } else {
            return true;
        }
    }

    // Check if the bottom notifications warning appears that you are not allowed to annotate 1.0 syntax pages
    public void waitforAnnotationWarningNotification()
    {
        waitForNotificationWarningMessage(XWIKI_SYNTAX_1_WARNING);
    }

    /**
     * @param annotationId the annotation id.
     * @return the comment id. Used by {@link org.xwiki.test.ui.po.CommentsTab}.
     * @since 10.6RC1
     * @since 9.11.9
     */
    public int getCommentId(String annotationId)
    {
        return Integer.valueOf(getDriver().findElement(By.xpath("//*[@id='_comments']" +
            "//div[contains(@class, 'annotation')][a[contains(@href,'#" + annotationId + "')]]"))
            .getAttribute("id").replace("xwikicomment_", ""));
    }

    /**
     * Search for an annotation by its id and return the corresponding web element.
     *
     * @param id the id of the annotation to search for
     * @return the web element of the requested annotation
     * @since 16.2.0RC1
     * @since 15.10.7
     */
    public WebElement getAnnotationTextById(int id)
    {
        return getDriver().findElement(By.cssSelector(String.format("span.annotation.ID%d", id)));
    }
}
