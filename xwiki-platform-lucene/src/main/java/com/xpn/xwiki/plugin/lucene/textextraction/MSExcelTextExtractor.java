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
package com.xpn.xwiki.plugin.lucene.textextraction;

import java.io.ByteArrayInputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class MSExcelTextExtractor implements MimetypeTextExtractor
{
    /** Logging helper. */
    private static final Log LOG = LogFactory.getLog(MSExcelTextExtractor.class);

    /** The currently preparing Excel workbook. */
    private HSSFWorkbook mWorkbook;

    /**
     * Contains all data formats used in the currently preparing Excel workbook.
     */
    private HSSFDataFormat mDataFormat;

    public static final int DEFAULT_BUFFER_SIZE = 16384; // 16 k

    /**
     * Extracts all text from an Excel by parsing all the sheets in that excel document.
     * 
     * @return String
     */
    public String getText(byte[] data) throws Exception
    {
        POIFSFileSystem poiFs = new POIFSFileSystem(new ByteArrayInputStream(data));
        mWorkbook = new HSSFWorkbook(poiFs);
        mDataFormat = mWorkbook.createDataFormat();

        StringBuffer cleanBuffer = new StringBuffer(DEFAULT_BUFFER_SIZE);
        for (int sheetIdx = 0; sheetIdx < mWorkbook.getNumberOfSheets(); sheetIdx++) {
            HSSFSheet sheet = mWorkbook.getSheetAt(sheetIdx);

            if (sheet != null) {
                parseSheet(sheet, cleanBuffer);
            }
        }
        return cleanBuffer.toString();
    }

    /**
     * It will parse the sheet with row wise and get the text from the sheet.
     */
    private void parseSheet(HSSFSheet sheet, StringBuffer cleanBuffer)
    {
        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
        for (int rowIdx = firstRow; rowIdx <= lastRow; rowIdx++) {
            HSSFRow row = sheet.getRow(rowIdx);

            if (row != null) {
                parseRow(row, cleanBuffer);
            }
        }
    }

    /**
     * It will parse row and return the text
     */
    private void parseRow(HSSFRow row, StringBuffer cleanBuffer)
    {
        short firstCell = row.getFirstCellNum();
        short lastCell = row.getLastCellNum();
        for (short cellIdx = firstCell; cellIdx <= lastCell; cellIdx++) {
            HSSFCell cell = row.getCell(cellIdx);

            if (cell != null) {
                parseCell(cell, cleanBuffer);
            }
        }
    }

    /**
     * Extracts all text from each cell of the sheet
     */
    private void parseCell(HSSFCell cell, StringBuffer cleanBuffer)
    {
        String cellValue = null;

        if (cell.getCellType() == HSSFCell.CELL_TYPE_STRING) {
            cellValue = cell.getRichStringCellValue().getString();
        } else if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
            HSSFCellStyle style = cell.getCellStyle();
            short formatId = style.getDataFormat();
            String formatPattern = mDataFormat.getFormat(formatId);
            formatPattern = replace(formatPattern, "\\ ", " ");

            if (isCellDateFormatted(cell)) {
                // This is a date
                formatPattern = replace(formatPattern, "mmmm", "MMMM");
                formatPattern = replace(formatPattern, "/", ".");
                SimpleDateFormat format;
                try {
                    format = new SimpleDateFormat(formatPattern);
                } catch (Throwable thr) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Creating date format failed: '" + formatPattern + "'", thr);
                    }
                    format = new SimpleDateFormat();
                }

                double numberValue = cell.getNumericCellValue();
                Date date = HSSFDateUtil.getJavaDate(numberValue);
                cellValue = format.format(date);
            } else {
                // This is a Number
                DecimalFormat format;
                try {
                    format = new DecimalFormat(formatPattern);
                } catch (Throwable thr) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Creating number format failed: '" + formatPattern + "'", thr);
                    }
                    format = new DecimalFormat();
                }

                double numberValue = cell.getNumericCellValue();
                cellValue = format.format(numberValue);
            }
        }

        if (cellValue != null) {
            cellValue = cellValue.trim();
            if (cellValue.length() != 0) {
                cleanBuffer.append(cellValue);
                cleanBuffer.append(" ");
            }
        }
    }

    /**
     * Checks cell is date formatted or not.
     * 
     * @return boolean
     */
    private boolean isCellDateFormatted(HSSFCell cell)
    {
        short format = cell.getCellStyle().getDataFormat();

        if (HSSFDateUtil.isValidExcelDate(cell.getNumericCellValue())) {
            if (HSSFDateUtil.isCellDateFormatted(cell)) {
                return true;
            } else {
                String fmtText = mDataFormat.getFormat(format);

                if (fmtText != null) {
                    fmtText = fmtText.toLowerCase();

                    if (fmtText.indexOf("d") >= 0 || fmtText.indexOf("m") >= 0
                        || fmtText.indexOf("y") >= 0 || fmtText.indexOf("h") >= 0
                        || fmtText.indexOf("s") >= 0) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * It will replace all occurances of pattern in the source with replacement value
     * 
     * @return String
     */
    public static String replace(String source, String pattern, String replacement)
    {
        // Check whether the pattern occurs in the source at all
        int firstPatternPos = source.indexOf(pattern);
        if (firstPatternPos == -1) {
            // The pattern does not occur in the source -> return the source
            return source;
        }

        // Build a new String where pattern is replaced by the replacement
        StringBuffer target = new StringBuffer(source.length());
        int start = 0; // The start of a part without the pattern
        int end = firstPatternPos; // The end of a part without the pattern
        do {
            target.append(source.substring(start, end));
            target.append(replacement);
            start = end + pattern.length();
        } while ((end = source.indexOf(pattern, start)) != -1);
        target.append(source.substring(start, source.length()));

        // return the String
        return target.toString();
    }
}
