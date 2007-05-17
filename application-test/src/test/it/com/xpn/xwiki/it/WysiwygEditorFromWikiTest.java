package com.xpn.xwiki.it;

/**
 * Tests the WYSIWYG editor (content edited in Wiki mode and then switched in WYSIWYG mode).
 *
 * @version $Id: $
 */
public class WysiwygEditorFromWikiTest extends AbstractTinyMceTestCase
{
    protected void setUp() throws Exception
    {
        super.setUp();
        open("/xwiki/bin/edit/Test/WysiwygEdit?editor=wiki");
    }

    public void testIndentedOrderedList() throws Exception
    {
        setFieldValue("content", "1. level 1\n11. level 2");
        clickLinkWithText("WYSIWYG");

        assertTinyMceHTMLContentExists("ol/li[text()='level 1']");
        assertTinyMceHTMLContentExists("ol/ol/li[text()='level 2']");

        clickLinkWithText("Wiki");
        assertEquals("1. level 1\n11. level 2", getFieldValue("content"));
    }
}
