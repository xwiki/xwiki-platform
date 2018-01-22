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
 * @since 4.2M1
 * @version $Id$
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
        selectText(annotatedText);
        simulateCTRL_M();
        annotationsWindow.addAnnotation(annotationText);
        // check is the saved successfully message is displayed
        waitForNotificationSuccessMessage(XWIKI_ANNOTATION_ADD_SUCCESS);
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

    // Shows the annotations pane from the top of the page
    public void showAnnotationsPane()
    {
        annotationsPane.showAnnotationsPane();
    }

    // Hides the annotations pane from the top of the page
    public void hideAnnotationsPane()
    {
        annotationsPane.hideAnnotationsPane();
    }

    // Checks the "Show Annotations" check box.
    public void clickShowAnnotations()
    {
        annotationsPane.clickShowAnnotations();
    }

    // Un-checks the "Show Annotations" check box.
    public void clickHideAnnotations()
    {
        annotationsPane.clickHideAnnotations();
    }

    // Checks if the checkBox within AnnotationsPane is visible
    public boolean checkIfClickbuttonExists()
    {
        return annotationsPane.checkIfShowAnnotationsCheckboxExists();
    }

    public void simulateCTRL_M()
    {
        WebElement body = getDriver().findElement(By.id("body"));
        body.sendKeys(Keys.chord(Keys.CONTROL, "m"));
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
        if (getDriver().findElementsWithoutWaiting(By.id("annotationsdisplay")).size() > 0)
            return false;
        else
            return true;
    }

    // Check if the bottom notifications warning appears that you are not allowed to annotate 1.0 syntax pages
    public void waitforAnnotationWarningNotification()
    {
        waitForNotificationWarningMessage(XWIKI_SYNTAX_1_WARNING);
    }
}
