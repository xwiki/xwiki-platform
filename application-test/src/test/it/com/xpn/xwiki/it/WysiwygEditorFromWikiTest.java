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

    public void testAutomaticConversionFromHashSyntaxToNumberSyntaxForOrderedLists()
    {
        setFieldValue("content", "# item 1\n## item 2\n# item 3");
        clickLinkWithText("WYSIWYG");
        clickLinkWithText("Wiki");

        assertEquals("1. item 1\n11. item 2\n1. item 3", getFieldValue("content"));
    }

    public void testHorizontalLineBeforeTableMacro()
    {
        setFieldValue("content", "----\n\n{table}\na | b\nc | d\n{table}");
        clickLinkWithText("WYSIWYG");
        clickLinkWithText("Wiki");

        assertEquals("----\n\n{table}\na | b\nc | d\n{table}", getFieldValue("content"));
    }

    public void testBulletedLists() throws Exception
    {
        setFieldValue("content", "- item 1\n-- item 2\n- item 3");
        clickLinkWithText("WYSIWYG");
        assertTinyMceHTMLContentExists("ul/li[text()='item 1']");
        assertTinyMceHTMLContentExists("ul/ul/li[text()='item 2']");
        assertTinyMceHTMLContentExists("ul/li[text()='item 3']");
        clickLinkWithText("Wiki");
        assertEquals("- item 1\n-- item 2\n- item 3", getFieldValue("content"));
    }
}
