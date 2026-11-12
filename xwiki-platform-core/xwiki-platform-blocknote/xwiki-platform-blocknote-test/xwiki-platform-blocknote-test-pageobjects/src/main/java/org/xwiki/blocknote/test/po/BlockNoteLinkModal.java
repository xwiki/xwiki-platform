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
package org.xwiki.blocknote.test.po;

import java.util.concurrent.atomic.AtomicReference;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the link modal used to create a link from the current selection or to edit an existing link.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
public class BlockNoteLinkModal extends BaseElement
{
    private static final String OPEN = "open";

    /**
     * The field designating the link target, whatever the selected target type.
     */
    private static final By TARGET_FIELD = By.cssSelector(".link-target input");

    /**
     * The link type selector, which is always displayed.
     */
    private static final By TARGET_TYPE_SELECT = By.cssSelector("[data-test='linkTargetType']");

    /**
     * The collapsible "Options" section, holding the display text field and the options specific to the selected
     * target type (e.g. the query string and the anchor of a page link, the subject and the body of an e-mail link).
     */
    private static final By OPTIONS = By.cssSelector("[data-test='linkOptions']");

    /**
     * The display text field, which is in the collapsible "Options" section.
     */
    private static final By DISPLAY_TEXT_FIELD = By.cssSelector("[data-test='linkDisplayText']");

    /**
     * The query string field of a page or an attachment link, which is in the collapsible "Options" section.
     */
    private static final By QUERY_STRING_FIELD =
        By.cssSelector("[data-test='linkPageQueryString'], [data-test='linkAttachmentQueryString']");

    /**
     * The anchor field of a page link, which is in the collapsible "Options" section.
     */
    private static final By ANCHOR_FIELD = By.cssSelector("[data-test='linkPageAnchor']");

    /**
     * The suggestions listed below the page and attachment search boxes, or the message replacing them (e.g. while
     * searching, or when nothing matches).
     */
    private static final By SUGGESTIONS = By.cssSelector(".link-target .suggestions-container");

    /**
     * The link modal itself, i.e. the outermost dialog holding the link type selector.
     */
    private static final By DIALOG =
        By.xpath("//*[@data-test = 'linkTargetType']/ancestor::*[@role = 'dialog'][last()]");

    /**
     * Create a new instance, waiting for the link modal to be visible.
     */
    public BlockNoteLinkModal()
    {
        getDriver().waitUntilCondition(ExpectedConditions.visibilityOfElementLocated(TARGET_TYPE_SELECT));
    }

    /**
     * Replaces the content of the link display text field with the given title, expanding the "Options" section that
     * holds it if needed.
     *
     * @param title the new link title
     */
    public void setDisplayText(String title)
    {
        expandOptions();
        replaceText(DISPLAY_TEXT_FIELD, title);
    }

    /**
     * Selects the type of target the link points to (e.g. {@code Page}, {@code URL}, {@code Attachment} or
     * {@code E-mail}).
     *
     * @param targetType the label of the target type to select
     */
    public void selectTargetType(String targetType)
    {
        WebElement targetTypeSelect = getDriver().findElement(TARGET_TYPE_SELECT);
        new Select(targetTypeSelect).selectByVisibleText(targetType);
    }

    /**
     * @return the label of the selected type of target (e.g. {@code Page}, {@code URL}, {@code Attachment} or
     *         {@code E-mail})
     * @since 18.10.0RC1
     */
    public String getTargetType()
    {
        WebElement targetTypeSelect = getDriver().findElement(TARGET_TYPE_SELECT);
        return new Select(targetTypeSelect).getFirstSelectedOption().getText();
    }

    /**
     * Replaces the content of the field designating the link target with the given text, whatever the selected target
     * type: e.g. the URL or the e-mail address. For the target types designated through a search box (page and
     * attachment), this only types the search query: use {@link #setPageReference(String, String)} or
     * {@link #setAttachmentReference(String, String, String)} to select the target.
     *
     * @param target the text to type
     * @since 18.10.0RC1
     */
    public void setTarget(String target)
    {
        replaceText(TARGET_FIELD, target);
    }

    /**
     * @return the content of the field designating the link target, whatever the selected target type: e.g. the URL,
     *         the page or attachment reference, or the e-mail address
     * @since 18.10.0RC1
     */
    public String getTarget()
    {
        return getDriver().findElement(TARGET_FIELD).getDomProperty(ATTRIBUTE_VALUE);
    }

    /**
     * @return the message the browser shows when the link modal is submitted while the field designating the link
     *         target is not valid (e.g. it's required but empty), or the empty string when this field is valid
     * @since 18.10.0RC1
     */
    public String getTargetValidationMessage()
    {
        return getDriver().findElement(TARGET_FIELD).getDomProperty("validationMessage");
    }

    /**
     * Types the given text where the focus is, without clicking anything first, as a keyboard user does right after the
     * link modal opens. Waits for the focus to be inside the link modal first, since the link modal moves it there
     * once it's shown.
     *
     * @param keysToSend the keys to type
     * @since 18.10.0RC1
     */
    public void typeInFocusedField(CharSequence... keysToSend)
    {
        WebElement dialog = getDriver().findElement(DIALOG);
        getDriver().waitUntilCondition(driver -> {
            WebElement activeElement = driver.switchTo().activeElement();
            return "input".equals(activeElement.getTagName())
                && (boolean) getDriver().executeScript("return arguments[0].contains(arguments[1])", dialog,
                    activeElement);
        });
        getDriver().switchTo().activeElement().sendKeys(keysToSend);
    }

    /**
     * Closes, with the Escape key, the suggestions listed below the page or attachment search box, once they are
     * shown (i.e. after typing in the search box), and waits for them to be hidden. The link modal is expected to
     * stay open.
     *
     * @since 18.10.0RC1
     */
    public void closeSuggestions()
    {
        getDriver().waitUntilElementIsVisible(SUGGESTIONS);
        getDriver().switchTo().activeElement().sendKeys(Keys.ESCAPE);
        getDriver().waitUntilElementDisappears(SUGGESTIONS);
    }

    /**
     * Replaces the query string of a page or an attachment link, expanding the "Options" section that holds it if
     * needed.
     *
     * @param queryString the new query string
     * @since 18.10.0RC1
     */
    public void setQueryString(String queryString)
    {
        expandOptions();
        replaceText(QUERY_STRING_FIELD, queryString);
    }

    /**
     * @return the query string of a page or an attachment link
     * @since 18.10.0RC1
     */
    public String getQueryString()
    {
        return getDriver().findElement(QUERY_STRING_FIELD).getDomProperty(ATTRIBUTE_VALUE);
    }

    /**
     * Replaces the anchor of a page link, expanding the "Options" section that holds it if needed.
     *
     * @param anchor the new anchor
     * @since 18.10.0RC1
     */
    public void setAnchor(String anchor)
    {
        expandOptions();
        replaceText(ANCHOR_FIELD, anchor);
    }

    /**
     * @return the anchor of a page link
     * @since 18.10.0RC1
     */
    public String getAnchor()
    {
        return getDriver().findElement(ANCHOR_FIELD).getDomProperty(ATTRIBUTE_VALUE);
    }

    /**
     * @return the content of the display text field, which is in the collapsible "Options" section
     * @since 18.10.0RC1
     */
    public String getDisplayText()
    {
        return getDriver().findElement(DISPLAY_TEXT_FIELD).getDomProperty(ATTRIBUTE_VALUE);
    }

    /**
     * Selects the {@code URL} target type, types the given URL and submits the modal.
     *
     * @param target the URL to set as the link target
     */
    public void setTargetAndSubmit(String target)
    {
        selectTargetType("URL");
        setTarget(target);
        submit();
    }

    /**
     * Types the given query in the page reference search box and selects the suggestion matching the given text. The
     * search box is only available when the target type is set to {@code Page}.
     *
     * @param query the text to type in the page reference search box
     * @param suggestionTitle the title of the suggestion to select
     */
    public void setPageReference(String query, String suggestionTitle)
    {
        setSuggestBoxQueryAndSelectSuggestion("linkPageReference", query, suggestionTitle, null);
    }

    /**
     * Selects the {@code Page} target type, sets the page reference (using the search box) and submits the modal.
     *
     * @param query the text to type in the page reference search box
     * @param suggestionTitle the title of the suggestion to select
     */
    public void setPageTargetAndSubmit(String query, String suggestionTitle)
    {
        selectTargetType("Page");
        setPageReference(query, suggestionTitle);
        submit();
    }

    /**
     * Types the given query in the attachment reference search box and selects the suggestion matching the given text.
     * The search box is only available when the target type is set to {@code Attachment}.
     *
     * @param query the text to type in the attachment reference search box
     * @param suggestionTitle the title of the suggestion to select
     * @param suggestionHint a fragment of the hint of the suggestion to select, used to disambiguate between
     *            suggestions with the same title
     */
    public void setAttachmentReference(String query, String suggestionTitle, String suggestionHint)
    {
        setSuggestBoxQueryAndSelectSuggestion("linkAttachmentReference", query, suggestionTitle, suggestionHint);
    }

    /**
     * Selects the {@code Attachment} target type, sets the attachment reference (using the search box) and submits the
     * modal.
     *
     * @param query the text to type in the attachment reference search box
     * @param suggestionTitle the title of the suggestion to select
     * @param suggestionHint a fragment of the hint of the suggestion to select, used to disambiguate between
     *            suggestions with the same title
     */
    public void setAttachmentTargetAndSubmit(String query, String suggestionTitle, String suggestionHint)
    {
        selectTargetType("Attachment");
        setAttachmentReference(query, suggestionTitle, suggestionHint);
        submit();
    }

    /**
     * Selects the {@code E-mail} target type, types the given e-mail address and submits the modal.
     *
     * @param address the e-mail address to set as the link target
     */
    public void setEmailTargetAndSubmit(String address)
    {
        selectTargetType("E-mail");
        setTarget(address);
        submit();
    }

    /**
     * Replaces the link title with the given value and submits the modal.
     *
     * @param title the new link title
     */
    public void setTitleAndSubmit(String title)
    {
        setDisplayText(title);
        submit();
    }

    /**
     * Submits the link modal, applying the changes, and waits for it to close.
     */
    public void submit()
    {
        trySubmit();
        waitUntilClosed();
    }

    /**
     * Tries to submit the link modal, without waiting for it to close: the browser blocks the submit when a required
     * field is empty, and the link modal then stays open.
     *
     * @since 18.10.0RC1
     */
    public void trySubmit()
    {
        getDriver().findElement(By.cssSelector("[data-test='linkSubmit']")).click();
    }

    /**
     * Cancels the link modal, discarding any change, and waits for it to close.
     */
    public void cancel()
    {
        getDriver().findElement(By.cssSelector("[data-test='linkCancel']")).click();
        waitUntilClosed();
    }

    private void waitUntilClosed()
    {
        getDriver().waitUntilElementDisappears(TARGET_TYPE_SELECT);
        // The backdrop of the link modal fades out once the link modal is closed, and it covers the edited content
        // until then.
        getDriver().waitUntilElementDisappears(By.cssSelector(".modal-backdrop"));
    }

    /**
     * Clicks outside the link modal, on the backdrop that covers the rest of the page (e.g. the edited content).
     *
     * @since 18.10.0RC1
     */
    public void clickOutside()
    {
        // The dialog covers the whole viewport, with its content inset from its edges: its top left corner is on the
        // backdrop.
        WebElement dialog = getDriver().findElement(DIALOG);
        getDriver().moveToTopLeftCornerOfTargetWithOffset(dialog, 5, 5, null).click().perform();
    }

    /**
     * Expands the "Options" section, unless it is already expanded, so that the option fields can be filled in: the
     * display text and the options specific to the selected target type (e.g. the query string and the anchor of a
     * page link, the subject and the body of an e-mail link).
     *
     * @since 18.10.0RC1
     */
    public void expandOptions()
    {
        WebElement options = getDriver().findElement(OPTIONS);
        if (!Boolean.parseBoolean(options.getDomProperty(OPEN))) {
            options.findElement(By.tagName("summary")).click();
            getDriver().waitUntilCondition(driver -> Boolean.parseBoolean(options.getDomProperty(OPEN)));
        }
    }

    /**
     * Replaces the content of the text field matching the given locator with the given text.
     *
     * @param locator the locator of the text field
     * @param text the text to type
     */
    private void replaceText(By locator, String text)
    {
        WebElement input = getDriver().findElement(locator);
        if (text.isEmpty()) {
            // Empty the field as a user does, because WebElement#clear() doesn't fire the input event that the field
            // listens to. The select all shortcut and the deletion are sent separately because Firefox keeps the
            // modifier key pressed for the rest of the keys sent along with a chord.
            input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            input.sendKeys(Keys.BACK_SPACE);
        } else {
            input.clear();
            input.sendKeys(text);
        }
    }

    /**
     * Types the given query in the search box (page or attachment reference) identified by the given {@code
     * data-test} attribute.
     *
     * @param dataTest the {@code data-test} attribute identifying the search box
     * @param query the text to type in the search box
     */
    private void setSuggestBoxQuery(String dataTest, String query)
    {
        WebElement queryInput = getDriver().findElement(By.cssSelector("[data-test='" + dataTest + "'] input"));
        queryInput.clear();
        queryInput.sendKeys(query);
    }

    /**
     * Types the given query in the search box (page or attachment reference) identified by the given {@code
     * data-test} attribute and waits for the suggestion with the given title to appear, then clicks on it.
     *
     * @param dataTest the {@code data-test} attribute identifying the search box
     * @param query the text to type in the search box
     * @param suggestionTitle the title of the suggestion to select
     * @param suggestionHint a fragment of the hint of the suggestion to select, used to disambiguate between
     *            suggestions with the same title
     */
    private void setSuggestBoxQueryAndSelectSuggestion(String dataTest, String query, String suggestionTitle,
        String suggestionHint)
    {
        String suggestionXPath = String.format("//*[@data-test = '%s']//li[contains(@class, 'suggestion')]"
            + "[.//span[contains(@class, 'suggestion-title') and . = '%s']]", dataTest, suggestionTitle);
        if (suggestionHint != null) {
            suggestionXPath +=
                String.format("[.//*[contains(@class, 'suggestion-hint') and contains(., '%s')]]", suggestionHint);
        }
        By suggestionLocator = By.xpath(suggestionXPath);
        // There's a race between the target page/attachment being saved and its indexing actually being queued, so
        // the search can return no results even though the Solr indexing queue was observed empty right before
        // typing the query. Retry the search (by retyping the query, which is required to trigger a new one) for up
        // to 30 seconds instead of just waiting longer for the initial search to eventually return the suggestion,
        // since the widget doesn't retry searches on its own. Retyping the query is also how we recover when the
        // suggestion is lost while we're trying to click it (see #selectSuggestion(By)).
        long deadline = System.currentTimeMillis() + 30000;
        while (true) {
            setSuggestBoxQuery(dataTest, query);
            try {
                getDriver().waitUntilElementIsVisible(suggestionLocator, 5);
                selectSuggestion(suggestionLocator);
                return;
            } catch (TimeoutException | StaleElementReferenceException e) {
                if (System.currentTimeMillis() >= deadline) {
                    throw e;
                }
            }
        }
    }

    /**
     * Clicks the suggestion matching the given locator, once the suggestion list has stopped changing.
     *
     * @param suggestionLocator the locator of the suggestion to select
     */
    private void selectSuggestion(By suggestionLocator)
    {
        // The search box starts a new search for each character typed in the query input and renders the results of
        // each of them as they come back, so the suggestion list can still be re-rendered after the expected
        // suggestion showed up. Clicking during such a re-render either loses the suggestion (stale element
        // reference) or sends the click where the suggestion no longer is. Wait for the suggestion element to be the
        // same across two consecutive checks before clicking it.
        AtomicReference<WebElement> previousSuggestion = new AtomicReference<>();
        WebElement suggestion = getDriver().waitUntilCondition(driver -> {
            WebElement currentSuggestion =
                driver.findElements(suggestionLocator).stream().findFirst().orElse(null);
            WebElement stableSuggestion =
                currentSuggestion != null && currentSuggestion.equals(previousSuggestion.get())
                    ? currentSuggestion : null;
            previousSuggestion.set(currentSuggestion);
            return stableSuggestion;
        });
        suggestion.click();
    }
}
