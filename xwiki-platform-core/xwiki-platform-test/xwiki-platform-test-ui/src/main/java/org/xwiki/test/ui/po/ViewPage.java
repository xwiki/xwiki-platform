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
package org.xwiki.test.ui.po;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

/**
 * Represents the common actions possible on all Pages when using the "view" action.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class ViewPage extends BasePage
{
    private static final String COMMENTS_TAB_ID = "Comments";

    private static final String HISTORY_TAB_ID = "History";

    private static final String INFORMATION_TAB_ID = "Information";

    @FindBy(id = "xwikicontent")
    private WebElement content;

    @FindBy(id = "hierarchy")
    private WebElement breadcrumbElement;

    private BreadcrumbElement breadcrumb;

    /**
     * @param id the tab identifier
     * @return {@code true} if the tab with the given id is available on this page, {@code false} otherwise
     * @since 17.9.0RC1
     * @since 17.4.6
     * @since 16.10.13
     */
    public boolean hasDocExtraPane(String id)
    {
        return getDriver().hasElementWithoutWaiting(By.id(id + "link"));
    }

    /**
     * Opens the document extra tab with the given id and returns its content.
     * 
     * @param id the tab identifier
     * @return the content of the specified document extra tab
     * @since 17.9.0RC1
     * @since 17.4.6
     * @since 16.10.13
     */
    public DocExtraPane openDocExtraPane(String id)
    {
        getDriver().findElement(By.id(id + "link")).click();
        waitForDocExtraPaneActive(id);
        return new DocExtraPane(id);
    }

    /**
     * @param id the tab identifier
     * @return {@code true} if the tab with the given id is currently active (visible), {@code false} otherwise
     * @since 17.9.0RC1
     * @since 17.4.6
     * @since 16.10.13
     */
    public boolean isDocExtraPaneActive(String id)
    {
        return getDriver().findElement(By.id(id + "pane")).isDisplayed();
    }

    /**
     * @param id the tab identifier
     */
    public void waitForDocExtraPaneActive(String id)
    {
        getDriver().waitUntilElementIsVisible(By.id(id + "pane"));
    }

    public DocExtraPane useShortcutForDocExtraPane(String id, CharSequence... shortcut)
    {
        // We send the shortcut to the active element because using the Actions API doesn't seem to work with key
        // combinations (like Shift+T, tested both on Firefox and Chrome).
        getDriver().switchTo().activeElement().sendKeys(shortcut);
        waitForDocExtraPaneActive(id);
        return new DocExtraPane(id);
    }

    /**
     * @return if the comments extra pane is available on this page
     * @since 17.7.0RC1
     * @since 17.4.3
     * @since 16.10.10
     */
    public boolean hasCommentsDocExtraPane()
    {
        return hasDocExtraPane(COMMENTS_TAB_ID);
    }

    /**
     * Opens the comments tab.
     * 
     * @return element for controlling the comments tab
     */
    public CommentsTab openCommentsDocExtraPane()
    {
        openDocExtraPane(COMMENTS_TAB_ID);
        return new CommentsTab();
    }

    public CommentsTab useShortcutKeyForCommentPane()
    {
        useShortcutForDocExtraPane(COMMENTS_TAB_ID, "c");
        return new CommentsTab();
    }

    /**
     * @return if the history extra pane is available on this page
     * @since 17.7.0RC1
     * @since 17.4.3
     * @since 16.10.10
     */
    public boolean hasHistoryDocExtraPane()
    {
        return hasDocExtraPane(HISTORY_TAB_ID);
    }

    public HistoryPane openHistoryDocExtraPane()
    {
        openDocExtraPane(HISTORY_TAB_ID);
        return new HistoryPane();
    }

    public HistoryPane useShortcutKeyForHistoryPane()
    {
        useShortcutForDocExtraPane(HISTORY_TAB_ID, "h");
        return new HistoryPane();
    }

    /**
     * @return if the information extra pane is available on this page
     * @since 17.7.0RC1
     * @since 17.4.3
     * @since 16.10.10
     */
    public boolean hasInformationDocExtraPane()
    {
        return hasDocExtraPane(INFORMATION_TAB_ID);
    }

    public InformationPane openInformationDocExtraPane()
    {
        openDocExtraPane(INFORMATION_TAB_ID);
        return new InformationPane();
    }

    public InformationPane useShortcutKeyForInformationPane()
    {
        useShortcutForDocExtraPane(INFORMATION_TAB_ID, "i");
        return new InformationPane();
    }

    /** @return does this page exist. */
    public boolean exists()
    {
        List<WebElement> messages = getDriver().findElementsWithoutWaiting(By.className("xwikimessage"));
        for (WebElement message : messages) {
            if (message.getText().contains("The requested page could not be found.")
                || message.getText().contains("The page has been deleted.")) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the page's main content as text (no HTML)
     */
    public String getContent()
    {
        return this.content.getText();
    }

    public WYSIWYGEditPage editSection(int sectionNumber)
    {
        By sectionBy = By.cssSelector("a.edit_section[href*=\"section=" + sectionNumber + "\"]");

        // Since Section Edit links are generated by JS (for XWiki Syntax 2.0) after the page has loaded make sure
        // we wait for them.
        getDriver().waitUntilElementIsVisible(sectionBy);

        getDriver().findElement(sectionBy).click();
        return new WYSIWYGEditPage();
    }

    /**
     * @return the "Translate" page button; use this only if you expect the button to be present
     * @since 12.10.6
     * @since 13.2RC1
     */
    public WebElement getTranslateButton()
    {
        return getDriver().findElement(By.cssSelector("#tmTranslate > a.btn"));
    }

    /**
     * @return {@code true} if the "Translate" page button is present, {@code false} otherwise
     * @since 12.10.6
     * @since 13.2RC1
     */
    public boolean hasTranslateButton()
    {
        return getDriver().hasElementWithoutWaiting(By.id("#tmTranslate"));
    }

    /**
     * Clicks on a wanted link in the page.
     */
    public void clickWantedLink(String spaceName, String pageName, boolean waitForTemplateDisplay)
    {
        clickWantedLink(new DocumentReference("xwiki", spaceName, pageName), waitForTemplateDisplay);
    }

    public CreatePagePage clickWantedLink(EntityReference reference)
    {
        return clickWantedLink(reference, true).get();
    }

    /**
     * Clicks on a wanted link in the page.
     *
     * @since 7.2M2
     */
    public Optional<CreatePagePage> clickWantedLink(EntityReference reference, boolean waitForTemplateDisplay)
    {
        WebElement brokenLink = getDriver().findElement(
            By.xpath("//span[@class='wikicreatelink']/a[contains(@href,'/create/" + getUtil().getURLFragment(reference)
                + "')]"));
        brokenLink.click();
        if (waitForTemplateDisplay) {
            // Ensure that the template choice popup is displayed. Since this is done using JS we need to wait till
            // it's displayed. For that we wait on the Create button since that would mean the template radio buttons
            // will all have been displayed.
            getDriver().waitUntilElementIsVisible(
                By.xpath("//div[@class='modal-dialog']//form[@id='create']//button[@type='submit']"));
            return Optional.of(new CreatePagePage());
        }
        return Optional.empty();
    }

    public BreadcrumbElement getBreadcrumb()
    {
        if (this.breadcrumb == null) {
            this.breadcrumb = new BreadcrumbElement(this.breadcrumbElement);
        }
        return this.breadcrumb;
    }

    public String getBreadcrumbContent()
    {
        return getBreadcrumb().getPathAsString();
    }

    public boolean hasBreadcrumbContent(String breadcrumbItem, boolean isCurrent)
    {
        return hasBreadcrumbContent(breadcrumbItem, isCurrent, true);
    }

    public boolean hasBreadcrumbContent(String breadcrumbItem, boolean isCurrent, boolean withLink)
    {
        return getBreadcrumb().hasPathElement(breadcrumbItem, isCurrent, withLink);
    }

    /**
     * Clicks on the breadcrumb link with the given text.
     * 
     * @param linkText the link text
     * @return the target of the breadcrumb link
     */
    public ViewPage clickBreadcrumbLink(String linkText)
    {
        getBreadcrumb().clickPathElement(linkText);

        return new ViewPage();
    }

    public boolean isInlinePage()
    {
        return getDriver().findElements(By.xpath("//form[@id = 'inline']")).size() > 0;
    }

    /**
     * Waits until the page has the passed content by refreshing the page
     * 
     * @param expectedValue the content value to wait for (in regex format), can be a subset of the full content
     * @since 4.0M1
     */
    public void waitUntilContent(final String expectedValue)
    {
        // Using an array to have an effectively final variable.
        final String[] lastContent = new String[1];
        try {
            getDriver().waitUntilCondition(new ExpectedCondition<Boolean>() {
                private Pattern pattern = Pattern.compile(expectedValue, Pattern.DOTALL);

                @Override
                public Boolean apply(WebDriver driver)
                {
                    // Note: don't refresh the page here since that would fail use cases (imagine some async process
                    // executing, the refresh will just start over that async process!). In addition, users don't need
                    // to click refresh so the tests shouldn't do that either.
                    lastContent[0] = getContent();
                    return Boolean.valueOf(pattern.matcher(lastContent[0]).find());
                }
            });
        } catch (TimeoutException e) {
            throw new TimeoutException(String.format("Got [%s]\nExpected [%s]", lastContent[0], expectedValue), e);
        }
    }

    /**
     * @param elementLocator the element to locate in the content of the page.
     * @return true if the content of the page contains the element
     * @since 11.5RC1
     */
    public boolean contentContainsElement(By elementLocator)
    {
        return getDriver().hasElementWithoutWaiting(this.content, elementLocator);
    }

    /**
     * Instantaneously scrolls to the top of the screen.
     *
     * @since 13.3RC1
     * @since 12.10.7
     */
    public void scrollToTop()
    {
        // scrollTo allows to move the view to the top of the page instantaneously, allowing to safely continue the
        // browser interactions without risks of seeing the screen moving up during the following test steps.
        // Note: this action was previously performed by sending a home key pressed event, which was not synchronous on 
        // Chrome, leading to flickering tests, notably when the scroll was followed by a drag and drop action.
        getDriver().scrollTo(0, 0);
    }

    public String getTitleColor()
    {
        return getElementCSSValue(By.id("document-title"), "color");
    }

    public String getPageBackgroundColor()
    {
        return getElementCSSValue(By.id("mainContentArea"), "background-color");
    }

    public String getTitleFontFamily()
    {
        return getElementCSSValue(By.id("document-title"), "font-family");
    }

    private String getElementCSSValue(By locator, String attribute)
    {
        return getDriver().findElement(locator).getCssValue(attribute);
    }

    /**
     * @return the last modified text displayed under the title in a wiki page
     * @since 15.1RC1
     */
    public String getLastModifiedText()
    {
        return getDriver().findElement(By.className("xdocLastModification")).getText();
    }

    /**
     * @param wait if {@code true} waits (with the standard timeout) until the required rights warning is present,
     *     otherwise returns immediately
     * @return {@code true} if the page has a required rights warning, {@code false} otherwise
     * @since 17.4.0RC1
     */
    public boolean hasRequiredRightsWarning(boolean wait)
    {
        By requiredRightsWarningSelector = By.cssSelector("#missing-required-rights-warning .requiredrights-warning");
        if (wait) {
            return getDriver().hasElementWithoutWaiting(requiredRightsWarningSelector);
        } else {
            return getDriver().hasElement(requiredRightsWarningSelector);
        }
    }

    /**
     * Opens the required rights modal by clicking on the button in the required rights warning.
     *
     * @return the opened required rights modal
     * @since 17.4.0RC1
     */
    public RequiredRightsModal openRequiredRightsModal()
    {
        WebElement reviewButton = getDriver().findElement(By.cssSelector("#missing-required-rights-warning button"));
        // Wait until the button isn't disabled anymore to avoid clicking the button before the event handler has been
        // initialized.
        getDriver().waitUntilCondition(driver -> reviewButton.isEnabled());
        reviewButton.click();
        return new RequiredRightsModal();
    }
}
