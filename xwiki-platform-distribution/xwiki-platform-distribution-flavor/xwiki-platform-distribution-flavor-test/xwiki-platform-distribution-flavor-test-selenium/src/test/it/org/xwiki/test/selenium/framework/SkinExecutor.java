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
package org.xwiki.test.selenium.framework;

/**
 * Skin-related actions to be implemented by the different Skin Executors. A Skin Executor is simply a class extending
 * this interface and implementing the listed UI actions.
 * 
 * @version $Id$
 */
public interface SkinExecutor
{
    /**
     * Click on the Edit button leading to the default edit mode for the page.
     */
    void clickEditPage();

    /**
     * Clicks on the link that leads to the wiki edit mode for the current page.
     * 
     * @since 2.5M1
     */
    void clickEditPageInWikiSyntaxEditor();

    /**
     * Clicks on the link that leads to the WYSIWYG edit mode for the current page.
     * 
     * @since 2.5M1
     */
    void clickEditPageInWysiwyg();

    /**
     * Clicks on the link that leads to the page access rights editor for the current page.
     * 
     * @since 4.3RC1
     */
    void clickEditPageAccessRights();
    
    /**
     * Clicks on the link that leads to the Inline Form edit mode for the current page.
     * 
     * @since 4.3RC1
     */
    void clickEditPageInlineForm();

    /**
     * Click on the Delete button leading to the page for deleting the current page.
     */
    void clickDeletePage();

    /**
     * Click on the Copy button leading to the page for copying the current page.
     */
    void clickCopyPage();

    /**
     * Click on the Show comments button leading to showing comments for the current page.
     */
    void clickShowComments();

    /**
     * Click on the Show attachments button leading to showing attachments for the current page.
     */
    void clickShowAttachments();

    /**
     * Click on the Show history button leading to showing history for the current page.
     */
    void clickShowHistory();

    /**
     * Click on the Show information button leading to showing information for the current page.
     */
    void clickShowInformation();

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
     * Clicks on the add property button in the class editor. As a result the specified property is added to the edited
     * class and the class is saved. This method waits for the class to be saved.
     */
    void clickEditAddProperty();

    /**
     * Clicks on the add object button in the object editor. As a result an object of the specified class is added to
     * the edited document and the document is saved. This method waits for the document to be saved.
     */
    void clickEditAddObject();

    /**
     * @return true if there's a user logged in or false otherwise
     */
    boolean isAuthenticated();

    /**
     * @param username the user to check if it's authenticated or not
     * @return {@code true} if the user specified by {@code username} is authenticated already, {@code false} otherwise
     */
    boolean isAuthenticated(String username);

    /**
     * @return {@code true} if the menu with login actions is present (login, logout, register, etc), {@code false}
     *         otherwise
     */
    boolean isAuthenticationMenuPresent();

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
     * Logs in the Admin user, if not already logged in.
     */
    void loginAsAdmin();

    /**
     * Click on the Login button leading to the login page.
     */
    void clickLogin();

    /**
     * Click on the Register button
     */
    void clickRegister();

    /**
     * @return The syntax used by the editor, examples: "xwiki/1.0", "xwiki/2.0".
     */
    String getEditorSyntax();

    /**
     * Set the syntax to use when editing a page.
     * 
     * @param syntax Syntax to use.
     */
    void setEditorSyntax(String syntax);

    /**
     * Edit the passed space/page using the Wiki editor.
     * 
     * @param space the space to which the page to edit belongs to
     * @param page the page to edit
     */
    void editInWikiEditor(String space, String page);

    /**
     * Edit the passed space/page using the Wiki editor.
     * 
     * @param space the space to which the page to edit belongs to
     * @param page the page to edit
     * @param syntax the syntax to use
     */
    void editInWikiEditor(String space, String page, String syntax);

    // For WYSIWYG editor

    /**
     * Edit the passed space/page using the WYSIWYG editor.
     * 
     * @param space the space to which the page to edit belongs to
     * @param page the page to edit
     */
    void editInWysiwyg(String space, String page);

    /**
     * Edit the passed space/page using the WYSIWYG editor.
     * 
     * @param space the space to which the page to edit belongs to
     * @param page the page to edit
     * @param syntax the syntax to use
     */
    void editInWysiwyg(String space, String page, String syntax);

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
     * Type the passed text in the Wiki editor.
     * 
     * @param text the text to be added to the Wiki editor content
     */
    void typeInWiki(String text);

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
     * Clicks the Wiki editor button to make the selected text bold, or to enter a bold marker if no text is selected.
     */
    void clickWikiBoldButton();

    /**
     * Clicks the Wiki editor button to make the selected text italics, or to enter an italics marker if no text is
     * selected.
     */
    void clickWikiItalicsButton();

    /**
     * Clicks the Wiki editor button to make the selected text underlined, or to enter an underline marker if no text is
     * selected.
     */
    void clickWikiUnderlineButton();

    /**
     * Clicks the Wiki editor button to turn the selected text into a link, or to enter a new link if no text is
     * selected.
     */
    void clickWikiLinkButton();

    /**
     * Clicks the Wiki editor button to insert a new horizontal ruler.
     */
    void clickWikiHRButton();

    /**
     * Clicks the Wiki editor button to insert an image macro.
     */
    void clickWikiImageButton();

    /**
     * Clicks the Wiki editor button to insert a signature.
     */
    void clickWikiSignatureButton();

    /**
     * Clicks the link to the wiki administration.
     * @since 7.2M3
     */
    void clickAdministerWiki();

    /**
     * Verify that the WYSIWYG editor has generated the passed text when the page is viewed in the Wiki editor.
     * 
     * @param text the text to verify
     */
    void assertWikiTextGeneratedByWysiwyg(String text);

    /**
     * Verify that the WYSIWYG editor has generated HTML content matching the passed XPath expression, without having to
     * save the edited document.
     * 
     * @param xpath the XPath expression to check
     * @throws Exception in case of a XPath parsing exception
     */
    void assertHTMLGeneratedByWysiwyg(String xpath) throws Exception;

    /**
     * Verify that the XWiki editor (be it Wiki or WYSIWYG) has generated HTML matching the passed XPath expression when
     * the document has been saved.
     * 
     * @param xpath the XPath expression to check
     * @throws Exception in case of a XPath parsing exception
     */
    void assertGeneratedHTML(String xpath) throws Exception;

    /**
     * Opens the wiki administration application homepage
     */
    void openAdministrationPage();

    /**
     * Opens an administration section in the wiki administration application
     */
    void openAdministrationSection(String section);

    /**
     * Press a key with optionnal keypress modifiers (Ctrl,Shift,etc)
     * 
     * @param shortcut the key to press
     * @param withCtrlModifier press Ctrl during shortcut key press
     * @param withAltModifier press Alt during shortcut key press
     * @param withShiftModifier press Shift during shortcut key press
     * @throws InterruptedException if selenium is interrupted during the key press
     */
    void pressKeyboardShortcut(String shortcut, boolean withCtrlModifier, boolean withAltModifier,
        boolean withShiftModifier) throws InterruptedException;

    /**
     * Tries to copy the specified page to the target page.
     * 
     * @param spaceName the name of the space containing the page to be copied
     * @param pageName the name of the page to be copied
     * @param targetSpaceName the name of the target space
     * @param targetPageName the name of the target page
     * @return {@code true} if the copy succeeded, {@code false} otherwise
     */
    boolean copyPage(String spaceName, String pageName, String targetSpaceName, String targetPageName);
}
