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
 *
 * @author Christian Gmeiner
 */
package com.xpn.xwiki.wysiwyg.server.converter.internal;

/**
 * This class represnets a so called State. In it are stored informations which are used for processing of found child
 * tags.
 * 
 * @version $Id: $
 */
public class State
{
    /**
     * @see #isOrderdList()
     */
    private boolean orderdList;

    /**
     * @see #isUnorderedList()
     */
    private boolean unorderedList;

    /**
     * @see #getInsertBefore()
     */
    private String insertBefore;

    /**
     * @see #getInsertAfter()
     */
    private String insertAfter;

    /**
     * @see #getTableColumnTotal()
     */
    private int tableColumnTotal;

    /**
     * @see #getTableColumnCount()
     */
    private int tableColumnCount;

    /**
     * @see #isExclude()
     */
    private boolean exclude;

    /**
     * @see #isStyle()
     */
    private boolean style;

    /**
     * @see #isInTable()
     */
    private boolean inTable;

    /**
     * Constructor.
     */
    public State()
    {
        this.orderdList = false;
        this.unorderedList = false;
        this.insertBefore = "";
        this.insertAfter = "";
        this.tableColumnTotal = 0;
        this.tableColumnCount = 0;
        this.exclude = false;
        this.style = false;
        this.inTable = false;
    }

    /**
     * Constructor.
     * 
     * @param other use parent State to setup this State object
     */
    public State(State other)
    {
        this.orderdList = other.isOrderdList();
        this.unorderedList = other.isUnorderedList();
        this.insertBefore = "";
        this.insertAfter = "";
        this.tableColumnTotal = other.getTableColumnTotal();
        this.tableColumnCount = other.getTableColumnCount();
        this.exclude = other.isExclude();
        this.style = other.isStyle();
        this.inTable = other.isInTable();
    }

    /**
     * @return the orderdList
     */
    public boolean isOrderdList()
    {
        return orderdList;
    }

    /**
     * @param orderdList the orderdList to set
     */
    public void setOrderdList(boolean orderdList)
    {
        this.orderdList = orderdList;
    }

    /**
     * @return the unorderedList
     */
    public boolean isUnorderedList()
    {
        return unorderedList;
    }

    /**
     * @param unorderedList the unorderedList to set
     */
    public void setUnorderedList(boolean unorderedList)
    {
        this.unorderedList = unorderedList;
    }

    /**
     * @return the insertAfter
     */
    public String getInsertAfter()
    {
        return insertAfter;
    }

    /**
     * @param insertAfter the insertAfter to set
     */
    public void setInsertAfter(String insertAfter)
    {
        this.insertAfter = insertAfter;
    }

    /**
     * @return the insertBefore
     */
    public String getInsertBefore()
    {
        return insertBefore;
    }

    /**
     * @param insertBefore the insertBefore to set
     */
    public void setInsertBefore(String insertBefore)
    {
        this.insertBefore = insertBefore;
    }

    /**
     * @return the tableColumnTotal
     */
    public int getTableColumnTotal()
    {
        return tableColumnTotal;
    }

    /**
     * @param tableColumnTotal the tableColumnTotal to set
     */
    public void setTableColumnTotal(int tableColumnTotal)
    {

        // it is only possible to change this value, if
        // it is 0. Else we could get some wired results
        // with table handling.
        if (this.tableColumnTotal == 0) {
            this.tableColumnTotal = tableColumnTotal;
        }
    }

    /**
     * @return the tableColumnCount, which is needed by the html2xwiki converter, to know when to place the | seperator
     *         between columns in tables.
     */
    public int getTableColumnCount()
    {
        return tableColumnCount;
    }

    /**
     * @param tableColumnCount the tableColumnCount to set
     */
    public void setTableColumnCount(int tableColumnCount)
    {
        this.tableColumnCount = tableColumnCount;
    }

    /**
     * Little helper to make it easier to increase tableColumnCount.
     */
    public void increaseTableColumnCount()
    {
        this.tableColumnCount++;
    }

    /**
     * @return if the the childs of the current node should be wirtten to the buffer or not
     */
    public boolean isExclude()
    {
        return exclude;
    }

    /**
     * @param exclude define, if the the childs of the current node should be wirtten to the buffer or not
     */
    public void setExclude(boolean exclude)
    {
        this.exclude = exclude;
    }

    /**
     * @return true, if the following #TEXT node contains styles to parse
     */
    public boolean isStyle()
    {
        return style;
    }

    /**
     * @param style does the following #TEXT node contains styles to parse?
     */
    public void setStyle(boolean style)
    {
        this.style = style;
    }

    /**
     * @return the inTable
     */
    public boolean isInTable()
    {
        return inTable;
    }

    /**
     * @param inTable the inTable to set
     */
    public void setInTable(boolean inTable)
    {
        this.inTable = inTable;
    }
}
