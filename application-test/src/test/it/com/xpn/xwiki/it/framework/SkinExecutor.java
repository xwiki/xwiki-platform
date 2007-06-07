/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.it.framework;

/**
 * Skin-related actions to be implemented by the different Skin Executors. A Skin Executor is
 * simply a class extending this interface and implementing the listed UI actions.
 *
 * @version $Id: $
 */
public interface SkinExecutor
{
    /**
     * Click on the Delete button leading to the page for deleting the current page.
     */
    void clickDeletePage();

    /**
     * Click on the Preview button in Edit mode to preview the changed made to a page.
     */
    void clickEditPreview();

    /**
     * Click on the Save & Continue button in Edit mode to save the page and continue editing it.
     */
    void clickEditSaveAndContinue();

    /**
     * Click on the Cancel button in Edit mode to cancel the modifications to a page.
     */
    void clickEditCancelEdition();

    /**
     * Click on the Save & View button in Edit mode to save the page and view the result.
     */
    void clickEditSaveAndView();

    /**
     * @return true if there's a user logged in or false otherwise
     */
    boolean isAuthenticated();

    /**
     * Logs out the current user.
     */
    void logout();

    /**
     * Login the passed user.
     *
     * @param username name of the user to log in
     * @param password password of the user to log in
     * @param rememberme if true the user will not have to log in again when he comes back
     */
    void login(String username, String password, boolean rememberme);

    /**
     * Logs in the Admin user.
     */
    void loginAsAdmin();

    /**
     * Click on the Login button leading to the log in page.
     */
    void clickLogin();

    // For WYSIWYG editor

    /**
     * Edit the passed space/page using the WYSIWYG editor.
     *
     * @param space the space to which the page to edit belongs to
     * @param page the page to edit
     */
    void editInWysiwyg(String space, String page);

    /**
     * Clears the content of the current page being edited in WYSIWYG mode
     */
    void clearWysiwygContent();

    /**
     * Type the passed text in the WYSIWYG editor.
     *
     * @param text the text to be added to the WYSIWYG editor content
     */
    void typeInWysiwyg(String text);

    /**
     * Press Enter in the WYSIWYG editor.
     */
    void typeEnterInWysiwyg();

    /**
     * Press Shift + Enter in the WYSIWYG editor.
     */
    void typeShiftEnterInWysiwyg();

    /**
     * Clicks the WYSIWYG editor button to removed an ordered list.
     */
    void clickWysiwygUnorderedListButton();

    /**
     * Clicks the WYSIWYG editor button to create an ordered list.
     */
    void clickWysiwygOrderedListButton();

    /**
     * Clicks the WYSIWYG editor button to indent the text at the cursor position.
     */
    void clickWysiwygIndentButton();

    /**
     * Clicks the WYSIWYG editor button to un-indent the text at the cursor position.
     */
    void clickWysiwygOutdentButton();

    /**
     * Verify that the WYSIWYG editor has generated the passed text when the page is viewed in
     * the Wiki editor.
     *
     * @param text the text to verify
     */
    void assertWikiTextGeneratedByWysiwyg(String text);

    /**
     * Verify that the WYSIWYG editor has generated HTML content matching the passed XPath
     * expression.
     *
     * @param xpath the XPath expression to check
     * @throws Exception in case of a XPath parsing exception
     */
    void assertHTMLGeneratedByWysiwyg(String xpath) throws Exception;
}
