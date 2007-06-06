package com.xpn.xwiki.it.framework;

public class AlbatrossSkinExecutor implements SkinExecutor
{
    private static final String WYSIWYG_LOCATOR_FOR_KEY_EVENTS = "mceSpanFonts";

    private AbstractXWikiTestCase test;

    public AlbatrossSkinExecutor(AbstractXWikiTestCase test)
    {
        this.test = test;
    }

    private AbstractXWikiTestCase getTest()
    {
        return this.test;        
    }

    public void clickDeletePage()
    {
        getTest().clickLinkWithLocator("//div[@id='tmDelete']/a");
    }

    public void clickEditPreview()
    {
       getTest().submit("formactionpreview");
    }

    public void clickEditSaveAndContinue()
    {
        getTest().submit("formactionsac");
    }

    public void clickEditCancelEdition()
    {
        getTest().submit("formactioncancel");
    }

    public void clickEditSaveAndView()
    {
        getTest().submit("formactionsave");
    }

    public boolean isAuthenticated()
    {
        return !(getTest().isElementPresent("headerlogin")
            && getTest().isElementPresent("headerregister"));
    }

    public void logout()
    {
        getTest().assertTrue("User wasn't authenticated.", isAuthenticated());
        getTest().clickLinkWithLocator("headerlogout");
        getTest().assertFalse("The user is always authenticated after a logout.", isAuthenticated());
    }

    public void login(String username, String password, boolean rememberme)
    {
        getTest().open("/xwiki/bin/view/Main/");

        if (isAuthenticated()) {
            logout();
        }

        clickLogin();

        getTest().setFieldValue("j_username", username);
        getTest().setFieldValue("j_password", password);
        if (rememberme) {
            getTest().checkField("rememberme");
        }
        getTest().submit();

        getTest().assertTrue("User has not been authenticated", isAuthenticated());
    }

    public void loginAsAdmin()
    {
        login("Admin", "admin", false);
    }

    public void clickLogin()
    {
        getTest().clickLinkWithLocator("headerlogin");
        assertIsLoginPage();
    }

    private void assertIsLoginPage()
    {
        getTest().assertElementPresent("loginForm");
        getTest().assertElementPresent("j_username");
        getTest().assertElementPresent("j_password");
        getTest().assertFalse(getTest().isChecked("rememberme"));
    }

    // For WYSIWYG editor

    public void editInWysiwyg(String space, String page)
    {
        getTest().open("/xwiki/bin/edit/" + space + "/" + page + "?editor=wysiwyg");
    }

    public void clearWysiwygContent()
    {
        getTest().getSelenium().waitForCondition(
            "selenium.browserbot.getCurrentWindow().tinyMCE.setContent(\"\"); true", "18000");
    }

    public void typeInWysiwyg(String text)
    {
        getTest().getSelenium().typeKeys(WYSIWYG_LOCATOR_FOR_KEY_EVENTS, text);
    }

    public void typeEnterInWysiwyg()
    {
        getTest().getSelenium().keyPress(WYSIWYG_LOCATOR_FOR_KEY_EVENTS, "\\13");
    }

    public void typeShiftEnterInWysiwyg()
    {
        getTest().getSelenium().shiftKeyDown();
        getTest().getSelenium().keyPress(WYSIWYG_LOCATOR_FOR_KEY_EVENTS, "\\13");
    }

    public void clickWysiwygUnorderedListButton()
    {
        getTest().clickLinkWithLocator("//img[@title='Unordered list']", false);
    }

    public void clickWysiwygOrderedListButton()
    {
        getTest().clickLinkWithLocator("//img[@title='Ordered list']", false);
    }

    public void clickWysiwygIndentButton()
    {
        getTest().clickLinkWithLocator("//img[@title='Indent']", false);
    }

    public void clickWysiwygOutdentButton()
    {
        getTest().clickLinkWithLocator("//img[@title='Outdent']", false);
    }

    public void assertWikiTextGeneratedByWysiwyg(String text)
    {
        getTest().clickLinkWithText("Wiki");
        getTest().assertEquals(text, getTest().getSelenium().getValue("content"));
    }

    public void assertHTMLGeneratedByWysiwyg(String xpath) throws Exception
    {
        String html = getTest().getSelenium().getEval("this.browserbot.getCurrentWindow().tinyMCE."
            + "getInstanceById('mce_editor_0').getDoc().body.innerHTML");
        getTest().assertXpathExists(xpath, html);
    }
}
