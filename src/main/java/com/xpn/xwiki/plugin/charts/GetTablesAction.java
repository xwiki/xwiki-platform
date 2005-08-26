package com.xpn.xwiki.plugin.charts;

import java.io.IOException;
import java.io.PrintWriter;

import org.radeox.macro.table.Table;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiAction;
import com.xpn.xwiki.web.XWikiResponse;

/**
 * This clas generates the tables for the new datasource wizard.
 * @author M
 *
 */
public class GetTablesAction extends XWikiAction{
    public String render(XWikiContext context) throws XWikiException {
        XWikiResponse response = context.getResponse();
        response.setContentType("text/xml");
        XWikiDocument doc = context.getDoc();
        try{
            PrintWriter writer = response.getWriter();
            RadeoxHelper rHelper = new RadeoxHelper(doc, context);
            Table[] tables = rHelper.getTables();

            writer.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
            writer.append("<html xmlns='http://www.w3.org/1999/xhtml'><head><title>Tables from the page " + doc.getFullName() + "</title></head><body><div id='dswEnvelope'>");
            for(int tidx = 0; tidx < tables.length; tidx++){
                Table t = tables[tidx];
                int rowCount = getTableRowCount(t);
                int colCount = getTableColumnCount(t);
                writer.append("<table>");
                for(int cidx = 0; cidx <= colCount + 1; cidx++){
                    writer.append("<col id='T" + tidx + "C" + cidx + "'/>");
                }
                writer.append("<tbody id='T" + tidx + "'><tr id='T" + tidx + "R0'>");
                writer.append("<th id='T" + tidx + "R0C0'>*</th>");
                for(int cidx = 0; cidx <= colCount; cidx++){
                    writer.append("<th id='T" + tidx + "R0C" + (cidx + 1) + "'>" + (char)((int)'A' + cidx) + "</th>");
                }
                writer.append("</tr>");
                for(int ridx = 0; ridx <= rowCount; ridx++){
                    writer.append("<tr id='T" + tidx + "R" + (ridx + 1) + "'>");
                    writer.append("<th id='T" + tidx + "R" + (ridx + 1) + "C0'>" + (ridx + 1) + "</th>");
                    for(int cidx = 0; cidx <= colCount; cidx++){
                        try{
                            writer.append("<td id='T" + tidx + "R" + (ridx + 1) + "C" + (cidx + 1) + "'>" + t.getXY(cidx, ridx).toString().replaceAll("<", "&lt;").replaceAll(">", "&gt;") + "</td>");
                        }
                        catch(Exception ex){
                            writer.append("<td id='T" + tidx + "R" + (ridx + 1) + "C" + (cidx + 1) + "'></td>");
                        }
                    }
                    writer.append("</tr>");
                }
                writer.append("</tbody></table>");
            }
            writer.append("</div></body></html>");
        }
        catch(IOException ex){}
        return null;
    }

    private int getTableColumnCount(Table t){
        int i = 0;
        try{
            while(true){
                t.getXY(i, 0);
                i++;
            }
        }
        catch(Exception ex){
            // Reached the table limit
            i--;
        }
        if(i < 0){
            return 0;
        }
        return i;
    }
    private int getTableRowCount(Table t){
        int i = 0;
        try{
            while(true){
                t.getXY(0, i);
                i++;
            }
        }
        catch(Exception ex){
            // Reached the table limit
            i--;
        }
        if(i < 0){
            return 0;
        }
        return i;
    }
}
