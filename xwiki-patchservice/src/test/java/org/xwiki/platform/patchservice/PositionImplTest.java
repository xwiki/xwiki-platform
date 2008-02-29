package org.xwiki.platform.patchservice;

import junit.framework.TestCase;

import org.xwiki.platform.patchservice.api.Position;
import org.xwiki.platform.patchservice.impl.PositionImpl;

public class PositionImplTest extends TestCase
{
    public void testCheckSimplePosition()
    {
        PositionImpl p = new PositionImpl(0, 0);
        assertTrue(p.checkPosition(null));
        assertTrue(p.checkPosition(""));
        assertTrue(p.checkPosition("anyhing"));
        assertTrue(p.checkPosition("anyhing\nand some more"));
        assertTrue(p.checkPosition("anyhing\nand some more\n"));
        assertTrue(p.checkPosition("\nanyhing\nand some more"));
        assertTrue(p.checkPosition("\n\n\n"));

        p = new PositionImpl(0, 5);
        assertFalse(p.checkPosition(null));
        assertFalse(p.checkPosition(""));
        assertTrue(p.checkPosition("some text"));
        assertFalse(p.checkPosition("some\ntext\n"));
        assertTrue(p.checkPosition("some \ntext\n"));
        assertFalse(p.checkPosition("so\nme\ntext\n"));
        assertTrue(p.checkPosition("   some text with spaces"));
        assertFalse(p.checkPosition("\n\n\n"));

        p = new PositionImpl(1, 0);
        assertTrue(p.checkPosition(null));
        assertTrue(p.checkPosition(""));
        assertTrue(p.checkPosition("\n"));
        assertTrue(p.checkPosition("some text"));
        assertTrue(p.checkPosition("some text\n"));
        assertTrue(p.checkPosition(" \n s\nome text with newlines"));
        assertTrue(p.checkPosition("first line\nsecond line\nthird line"));
        assertTrue(p.checkPosition("\n\n\n"));

        p = new PositionImpl(1, 5);
        assertFalse(p.checkPosition(null));
        assertFalse(p.checkPosition(""));
        assertFalse(p.checkPosition("\n"));
        assertFalse(p.checkPosition("some text"));
        assertFalse(p.checkPosition("some text\n"));
        assertTrue(p.checkPosition("some text\nsome more text"));
        assertTrue(p.checkPosition("some text\nsome more text\n"));
        assertFalse(p.checkPosition("some text\n\nsome mbore text"));
        assertFalse(p.checkPosition("some text\n\nsome more text\n"));
        assertFalse(p.checkPosition(" \n s\nome text with newlines"));
        assertTrue(p.checkPosition("first line\nsecond line\nthird line"));
        assertFalse(p.checkPosition("\n\n\n"));
    }

    public void testCheckPositionWithBefore()
    {
        PositionImpl p = new PositionImpl(0, 0, null, null);
        assertTrue(p.checkPosition(null));
        assertTrue(p.checkPosition(""));
        assertTrue(p.checkPosition("anyhing"));
        assertTrue(p.checkPosition("\n\n\n"));

        p = new PositionImpl(0, 0, "", "");
        assertTrue(p.checkPosition(null));
        assertTrue(p.checkPosition(""));
        assertTrue(p.checkPosition("anyhing"));
        assertTrue(p.checkPosition("\n\n\n"));

        p = new PositionImpl(0, 0, "before", null);
        assertFalse(p.checkPosition(null));
        assertFalse(p.checkPosition(""));
        assertFalse(p.checkPosition("anyhing"));
        assertFalse(p.checkPosition("\n\n\n"));

        p = new PositionImpl(1, 0, "before", null);
        assertFalse(p.checkPosition(null));
        assertFalse(p.checkPosition(""));
        assertFalse(p.checkPosition("before"));
        assertFalse(p.checkPosition("before\n"));

        p = new PositionImpl(1, 0, "before\n", null);
        assertTrue(p.checkPosition("before"));
        assertFalse(p.checkPosition("efore\n"));
        assertTrue(p.checkPosition("before\n"));
        assertTrue(p.checkPosition("before\nafter"));
        assertTrue(p.checkPosition("more before\n"));

        p = new PositionImpl(1, 6, "before", null);
        assertFalse(p.checkPosition("before"));
        assertFalse(p.checkPosition("before\n"));
        assertTrue(p.checkPosition("\nbefore\n"));
        assertFalse(p.checkPosition("\nefore this\n"));
        assertTrue(p.checkPosition("\nbefore\n"));
        assertFalse(p.checkPosition("\n before\n"));
        assertTrue(p.checkPosition("before\nbefore\nafter"));
        assertTrue(p.checkPosition("more\nbefore and after\n"));

        p = new PositionImpl(1, 4, "be\nfore", null);
        assertFalse(p.checkPosition("before"));
        assertFalse(p.checkPosition("before\n"));
        assertTrue(p.checkPosition("be\nfore\n"));
        assertFalse(p.checkPosition("\nbefore this\n"));
        assertTrue(p.checkPosition("be\nfore\n"));
        assertTrue(p.checkPosition("more be\nfore\n"));
        assertTrue(p.checkPosition("beforebe\nfore\nafter"));
        assertTrue(p.checkPosition("more be\nfore and after\n"));
    }

    public void testCheckPositionWithAfter()
    {
        PositionImpl p = new PositionImpl(0, 0, null, null);
        assertTrue(p.checkPosition(null));
        assertTrue(p.checkPosition(""));
        assertTrue(p.checkPosition("anyhing"));
        assertTrue(p.checkPosition("\n\n\n"));

        p = new PositionImpl(0, 0, "", "");
        assertTrue(p.checkPosition(null));
        assertTrue(p.checkPosition(""));
        assertTrue(p.checkPosition("anyhing"));
        assertTrue(p.checkPosition("\n\n\n"));

        p = new PositionImpl(0, 0, null, "after");
        assertFalse(p.checkPosition(null));
        assertFalse(p.checkPosition(""));
        assertFalse(p.checkPosition("anyhing"));
        assertFalse(p.checkPosition("\n\n\n"));
        assertTrue(p.checkPosition("after"));
        assertTrue(p.checkPosition("after and some more"));

        p = new PositionImpl(1, 0, null, "after");
        assertFalse(p.checkPosition(null));
        assertFalse(p.checkPosition(""));
        assertFalse(p.checkPosition("before"));
        assertFalse(p.checkPosition("before\n"));
        assertTrue(p.checkPosition("before\nafter"));
        assertTrue(p.checkPosition("before\nafter\n"));
        assertFalse(p.checkPosition("before\n after"));
        assertFalse(p.checkPosition("before\n\nafter"));

        p = new PositionImpl(1, 0, null, "\nafter");
        assertFalse(p.checkPosition("after"));
        assertFalse(p.checkPosition("\nafter"));
        assertTrue(p.checkPosition("\n\nafter"));
        assertTrue(p.checkPosition("before\n\nafter"));
        assertTrue(p.checkPosition("\n\nafter and more"));

        p = new PositionImpl(1, 6, null, "after");
        assertFalse(p.checkPosition("before\nafter"));
        assertFalse(p.checkPosition("before\nmiddle\nafter"));
        assertTrue(p.checkPosition("\nbeforeafter\n"));
        assertTrue(p.checkPosition("\nbeforeafter\nmore after"));
        assertTrue(p.checkPosition("\nbeforeafter more after"));
        assertFalse(p.checkPosition("\nbefore after"));
        assertTrue(p.checkPosition("more\n      after and more after\n"));

        p = new PositionImpl(1, 2, null, "af\nter");
        assertFalse(p.checkPosition("before\n  after"));
        assertFalse(p.checkPosition("before\n"));
        assertTrue(p.checkPosition("\n  af\nter"));
        assertTrue(p.checkPosition("befor\ne af\nter and more\n"));
    }
    
    public void testCheckPositionWithBeforeAndAfter()
    {
        Position p = new PositionImpl(1, 6, "before", "after");
        assertFalse(p.checkPosition("before\nafter"));
        assertTrue(p.checkPosition("\nbeforeafter"));
        assertTrue(p.checkPosition("\nbeforeafter\n"));
        assertTrue(p.checkPosition("more \nbeforeafter and more"));
        assertTrue(p.checkPosition("more \nbeforeafter\n and more"));
    }

    public void testTextBefore()
    {
        PositionImpl p = new PositionImpl(0, 0);
        assertEquals("", p.getTextBeforePosition(null));
        assertEquals("", p.getTextBeforePosition(""));
        assertEquals("", p.getTextBeforePosition("anyhing"));

        p = new PositionImpl(0, 5);
        assertEquals("", p.getTextBeforePosition(null));
        assertEquals("", p.getTextBeforePosition(""));
        assertEquals("some ", p.getTextBeforePosition("some text"));
        assertEquals("some", p.getTextBeforePosition("some\ntext\n"));
        assertEquals("so", p.getTextBeforePosition("so\nme\ntext\n"));
        assertEquals("   so", p.getTextBeforePosition("   some text with spaces"));
        assertEquals(" ", p.getTextBeforePosition(" \n s\nome text with newlines"));

        p = new PositionImpl(1, 0);
        assertEquals("\n", p.getTextBeforePosition(null));
        assertEquals("\n", p.getTextBeforePosition(""));
        assertEquals("\n", p.getTextBeforePosition("\n"));
        assertEquals("some text\n", p.getTextBeforePosition("some text"));
        assertEquals("some text\n", p.getTextBeforePosition("some text\n"));
        assertEquals(" \n", p.getTextBeforePosition(" \n s\nome text with newlines"));
        assertEquals("first line\n", p
            .getTextBeforePosition("first line\nsecond line\nthird line"));

        p = new PositionImpl(1, 5);
        assertEquals("\n", p.getTextBeforePosition(null));
        assertEquals("\n", p.getTextBeforePosition(""));
        assertEquals("\n", p.getTextBeforePosition("\n"));
        assertEquals("some text\n", p.getTextBeforePosition("some text"));
        assertEquals("some text\n", p.getTextBeforePosition("some text\n"));
        assertEquals("some text\n", p.getTextBeforePosition("some text\n\nsome more text"));
        assertEquals(" \n s", p.getTextBeforePosition(" \n s\nome text with newlines"));
        assertEquals("first line\nsecon", p
            .getTextBeforePosition("first line\nsecond line\nthird line"));
    }

    public void testTextAfter()
    {
        PositionImpl p = new PositionImpl(0, 0);
        assertEquals("", p.getTextAfterPosition(null));
        assertEquals("", p.getTextAfterPosition(""));
        assertEquals("anyhing", p.getTextAfterPosition("anyhing"));
        assertEquals("anyhing\nand some more", p.getTextAfterPosition("anyhing\nand some more"));
        assertEquals("anyhing\nand some more\n", p
            .getTextAfterPosition("anyhing\nand some more\n"));
        assertEquals("\nanyhing\nand some more", p
            .getTextAfterPosition("\nanyhing\nand some more"));
        assertEquals("\n\n\n", p.getTextAfterPosition("\n\n\n"));

        p = new PositionImpl(0, 5);
        assertEquals("", p.getTextAfterPosition(null));
        assertEquals("", p.getTextAfterPosition(""));
        assertEquals("text", p.getTextAfterPosition("some text"));
        assertEquals("\ntext\n", p.getTextAfterPosition("some\ntext\n"));
        assertEquals("\nme\ntext\n", p.getTextAfterPosition("so\nme\ntext\n"));
        assertEquals("me text with spaces", p.getTextAfterPosition("   some text with spaces"));
        assertEquals("\n\n\n", p.getTextAfterPosition("\n\n\n"));

        p = new PositionImpl(1, 0);
        assertEquals("", p.getTextAfterPosition(null));
        assertEquals("", p.getTextAfterPosition(""));
        assertEquals("", p.getTextAfterPosition("\n"));
        assertEquals("", p.getTextAfterPosition("some text"));
        assertEquals("", p.getTextAfterPosition("some text\n"));
        assertEquals(" s\nome text with newlines", p
            .getTextAfterPosition(" \n s\nome text with newlines"));
        assertEquals("second line\nthird line", p
            .getTextAfterPosition("first line\nsecond line\nthird line"));
        assertEquals("\n\n", p.getTextAfterPosition("\n\n\n"));

        p = new PositionImpl(1, 5);
        assertEquals("", p.getTextAfterPosition(null));
        assertEquals("", p.getTextAfterPosition(""));
        assertEquals("", p.getTextAfterPosition("\n"));
        assertEquals("", p.getTextAfterPosition("some text"));
        assertEquals("", p.getTextAfterPosition("some text\n"));
        assertEquals("more text", p.getTextAfterPosition("some text\nsome more text"));
        assertEquals("more text\n", p.getTextAfterPosition("some text\nsome more text\n"));
        assertEquals("\nsome more text", p.getTextAfterPosition("some text\n\nsome more text"));
        assertEquals("\nsome more text\n", p
            .getTextAfterPosition("some text\n\nsome more text\n"));
        assertEquals("\nome text with newlines", p
            .getTextAfterPosition(" \n s\nome text with newlines"));
        assertEquals("d line\nthird line", p
            .getTextAfterPosition("first line\nsecond line\nthird line"));
        assertEquals("\n\n", p.getTextAfterPosition("\n\n\n"));
    }
}
