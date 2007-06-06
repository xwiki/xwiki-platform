package com.xpn.xwiki.it.framework;

public interface SkinExecutor
{
    void clickDeletePage();
    
    void clickEditPreview();
    void clickEditSaveAndContinue();
    void clickEditCancelEdition();
    void clickEditSaveAndView();

    boolean isAuthenticated();

    void logout();
    void login(String username, String password, boolean rememberme);
    void loginAsAdmin();
    void clickLogin();

    // For WYSIWYG editor
    void editInWysiwyg(String space, String page);
    void clearWysiwygContent();
    void typeInWysiwyg(String text);
    void typeEnterInWysiwyg();
    void typeShiftEnterInWysiwyg();
    void clickWysiwygUnorderedListButton();
    void clickWysiwygOrderedListButton();
    void clickWysiwygIndentButton();
    void clickWysiwygOutdentButton();
    void assertWikiTextGeneratedByWysiwyg(String text);
    void assertHTMLGeneratedByWysiwyg(String xpath) throws Exception;
}
