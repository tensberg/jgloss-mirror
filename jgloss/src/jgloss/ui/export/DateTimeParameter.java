package jgloss.ui.export;

import jgloss.ui.JGlossFrame;

import java.net.URL;
import java.util.Date;
import java.text.DateFormat;

import org.w3c.dom.Element;

class DateTimeParameter extends AbstractParameter {
    private static final DateFormat DATETIME_FORMAT = 
        DateFormat.getDateTimeInstance( DateFormat.MEDIUM, DateFormat.SHORT);

    DateTimeParameter( Element elem) {
        super( elem);
    }

    public String getValue( JGlossFrame source, URL systemId) {
        return DATETIME_FORMAT.format( new Date( System.currentTimeMillis()));
    }
} // class DateTimeParameter
