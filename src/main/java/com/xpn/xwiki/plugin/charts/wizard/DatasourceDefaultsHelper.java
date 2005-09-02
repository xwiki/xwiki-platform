package com.xpn.xwiki.plugin.charts.wizard;

import com.xpn.xwiki.plugin.charts.source.TableDataSource;

/**
 * 
 * @author Sergiu Dumitriu
 *
 */
public class DatasourceDefaultsHelper {
    public String getDefaultTableNumber(){
        return TableDataSource.DEFAULT_TABLE_NUMBER + "";
    }

    public String getDefaultRange(){
        return TableDataSource.DEFAULT_RANGE + "";
    }

    public String getDefaultHasHeaderRow(){
        return TableDataSource.DEFAULT_HAS_HEADER_ROW + "";
    }

    public String getDefaultHasHeaderColumn(){
        return TableDataSource.DEFAULT_HAS_HEADER_COLUMN + "";
    }

    public String getDefaultDecimalSymbol(){
        return TableDataSource.DEFAULT_DECIMAL_SYMBOL + "";
    }

    public String getDefaultIgnoreAlpha(){
        return TableDataSource.DEFAULT_IGNORE_ALPHA + "";
    }
}
