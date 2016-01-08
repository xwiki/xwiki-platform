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
package com.xpn.xwiki.plugin.diff;

import com.xpn.xwiki.XWikiException;

import java.util.List;

import org.suigeneris.jrcs.diff.delta.Chunk;
import org.suigeneris.jrcs.diff.delta.Delta;

/**
 * Created by IntelliJ IDEA. User: ldubost Date: 1 mai 2007 Time: 15:06:14 To change this template
 * use File | Settings | File Templates.
 */
public class DiffTest extends org.jmock.cglib.MockObjectTestCase
{
    private DiffPlugin plugin;

    @Override
    protected void setUp()
    {
        this.plugin = new DiffPlugin("diff", DiffPlugin.class.getName(), null);
    }

    public void testSimpleLineDiff() throws XWikiException
    {
        String text1 = "A";
        String text2 = "A B";
        List diffs = this.plugin.getDifferencesAsList(text1, text2);
        assertEquals("There should be one difference", 1, diffs.size());
        Delta delta = (Delta) diffs.get(0);
        Chunk orig = delta.getOriginal();
        Chunk revised = delta.getRevised();
        assertEquals("Original should be", "A", orig.toString());
        assertEquals("Revised should be", "A B", revised.toString());
    }

    public void testSimpleLineDiff2() throws XWikiException
    {
        String text1 = "A\nB\nC";
        String text2 = "A\nB B\nC";
        List diffs = this.plugin.getDifferencesAsList(text1, text2);
        assertEquals("There should be one difference", 1, diffs.size());
        Delta delta = (Delta) diffs.get(0);
        Chunk orig = delta.getOriginal();
        Chunk revised = delta.getRevised();
        assertEquals("Original should be", "B", orig.toString());
        assertEquals("Revised should be", "B B", revised.toString());
    }

    public void testSimpleWordDiff() throws XWikiException
    {
        String text1 = "I love Paris";
        String text2 = "I live in Paris";
        List diffs = this.plugin.getWordDifferencesAsList(text1, text2);
        assertEquals("There should be two differences", 1, diffs.size());
        Delta delta1 = (Delta) diffs.get(0);
        Chunk orig1 = delta1.getOriginal();
        Chunk revised1 = delta1.getRevised();
        Delta delta2 = (Delta) diffs.get(0);
        Chunk orig2 = delta2.getOriginal();
        Chunk revised2 = delta2.getRevised();
        assertEquals("Original 1 should be", "love", orig1.toString());
        assertEquals("Revised 1 should be", "livein", revised1.toString());
    }

    public void testSimpleWordDiff2() throws XWikiException
    {
        String text1 = "I love Paris and London";
        String text2 = "I live in Paris and London";
        List diffs = this.plugin.getWordDifferencesAsList(text1, text2);
        assertEquals("There should be two differences", 1, diffs.size());
        Delta delta1 = (Delta) diffs.get(0);
        Chunk orig1 = delta1.getOriginal();
        Chunk revised1 = delta1.getRevised();

        assertEquals("Original 1 should be", "love", orig1.toString());
        assertEquals("Revised 1 should be", "livein", revised1.toString());
    }

    public void testSimpleWordDiff3() throws XWikiException
    {
        String text1 = "I love Paris and London";
        String text2 = "I love London and Paris";
        List diffs = this.plugin.getWordDifferencesAsList(text1, text2);
        assertEquals("There should be two differences", 2, diffs.size());
        Delta delta1 = (Delta) diffs.get(0);
        Chunk orig1 = delta1.getOriginal();
        Chunk revised1 = delta1.getRevised();
        Delta delta2 = (Delta) diffs.get(1);
        Chunk orig2 = delta2.getOriginal();
        Chunk revised2 = delta2.getRevised();

        assertEquals("Original 1 should be", "Parisand", orig1.toString());
        assertEquals("Revised 1 should be", "", revised1.toString());
        assertEquals("Original 2 should be", "", orig2.toString());
        assertEquals("Revised 2 should be", "andParis", revised2.toString());
    }

    public void testSimpleWordDiff4() throws XWikiException
    {
        String text1 = "I love Paris and I like London";
        String text2 = "I love London and I like Paris";
        List diffs = this.plugin.getWordDifferencesAsList(text1, text2);
        assertEquals("There should be two differences", 2, diffs.size());
        Delta delta1 = (Delta) diffs.get(0);
        Chunk orig1 = delta1.getOriginal();
        Chunk revised1 = delta1.getRevised();
        Delta delta2 = (Delta) diffs.get(1);
        Chunk orig2 = delta2.getOriginal();
        Chunk revised2 = delta2.getRevised();

        assertEquals("Original 1 should be", "Paris", orig1.toString());
        assertEquals("Revised 1 should be", "London", revised1.toString());
        assertEquals("Original 2 should be", "London", orig2.toString());
        assertEquals("Revised 2 should be", "Paris", revised2.toString());
    }

    public void testSimpleWordDiffAsHTML() throws XWikiException
    {
        String text1 = "A";
        String text2 = "A B";
        String html = this.plugin.getWordDifferencesAsHTML(text1, text2);
        assertEquals(
            "Diff is incorrect",
            "<div class=\"diffmodifiedline\"><span class=\"diffremoveword\">A</span><span class=\"diffaddword\">A B</span></div>",
            html);
    }

    public void testSimpleWordDiffAsHTML2() throws XWikiException
    {
        String text1 = "A C";
        String text2 = "A B";
        String html = this.plugin.getWordDifferencesAsHTML(text1, text2);
        assertEquals(
            "Diff is incorrect",
            "<div class=\"diffmodifiedline\">A <span class=\"diffremoveword\">C</span><span class=\"diffaddword\">B</span></div>",
            html);
    }

    public void testSimpleWordDiffAsHTML3() throws XWikiException
    {
        String text1 = "A B C D E F";
        String text2 = "A C B D E G";
        String html = this.plugin.getWordDifferencesAsHTML(text1, text2);
        assertEquals(
            "Diff is incorrect",
            "<div class=\"diffmodifiedline\">A <span class=\"diffremoveword\">B</span> C <span class=\"diffaddword\">B</span> D E <span class=\"diffremoveword\">F</span><span class=\"diffaddword\">G</span></div>",
            html);
    }

    public void testSimpleLineDiffAsHTML2() throws XWikiException
    {
        String text1 = "A C";
        String text2 = "A B";
        String html = this.plugin.getDifferencesAsHTML(text1, text2);
        assertEquals(
            "Diff is incorrect",
            "<div class=\"diff\"><div class=\"diffmodifiedline\">A <span class=\"diffremoveword\">C</span><span class=\"diffaddword\">B</span></div></div>",
            html);
    }

    public void testSimpleLineDiffAsHTML3() throws XWikiException
    {
        String text1 = "A B C D E F";
        String text2 = "A C B D E G";
        String html = this.plugin.getDifferencesAsHTML(text1, text2);
        assertEquals(
            "Diff is incorrect",
            "<div class=\"diff\"><div class=\"diffmodifiedline\">A <span class=\"diffremoveword\">B</span> C <span class=\"diffaddword\">B</span> D E <span class=\"diffremoveword\">F</span><span class=\"diffaddword\">G</span></div></div>",
            html);
    }

    public void testSimpleLineDiffAsHTML4() throws XWikiException
    {
        String text1 = "A B C\nD E F\nG H I\nJ K L\n";
        String text2 = "A B C\nG H I\nD E F\nJ K L\n";
        String html = this.plugin.getDifferencesAsHTML(text1, text2);
        assertEquals(
            "Diff is incorrect",
            "<div class=\"diff\"><div class=\"diffunmodifiedline\">A B C</div><div class=\"diffmodifiedline\"><span class=\"diffremoveword\">D E F</span></div><div class=\"diffunmodifiedline\">G H I</div><div class=\"diffmodifiedline\"><span class=\"diffaddword\">D E F</span></div><div class=\"diffunmodifiedline\">J K L</div></div>",
            html);
    }

    public void testMultiLineDiffAsHTML() throws XWikiException
    {
        String text1 = "A\n";
        String text2 = "AA\nAB\n";
        String html = this.plugin.getDifferencesAsHTML(text1, text2);
        assertEquals(
            "Diff is incorrect",
            "<div class=\"diff\"><div class=\"diffmodifiedline\"><span class=\"diffremoveword\">A</span><span class=\"diffaddword\">AA</span></div><div class=\"diffmodifiedline\"><span class=\"diffaddword\">AB</span></div></div>",
            html);
    }
}
