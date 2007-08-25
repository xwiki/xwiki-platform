package com.xpn.xwiki.xmlrpc;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.xpn.xwiki.XWikiException;

public class Convert
{
    public static class ConversionException extends XWikiException
    {
        public ConversionException(String message)
        {
            super(MODULE_XWIKI_XMLRPC, ERROR_XWIKI_UNKNOWN, message);
        }

        public ConversionException(Throwable cause)
        {
            super();
            setException(cause);
        }

        public ConversionException(String message, Throwable cause)
        {
            super(MODULE_XWIKI_XMLRPC, ERROR_XWIKI_UNKNOWN, message, cause);
        }

        private static final long serialVersionUID = -764605004981978909L;
    }

    public static String int2str(int i)
    {
        return (new Integer(i)).toString();
    }

    public static int str2int(String string) throws ConversionException
    {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            throw new ConversionException(e);
        }
    }

    public static String bool2str(boolean b)
    {
        return (new Boolean(b)).toString();
    }

    public static boolean str2bool(String string)
    {
        return Boolean.parseBoolean(string);
    }

    public static String date2str(Date date)
    {
        return format.format(date);
    }

    public static Date str2date(String string) throws ConversionException
    {
        try {
            return format.parse(string);
        } catch (ParseException e) {
            throw new ConversionException(e);
        }
    }

    private final static DateFormat format = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy");
}
