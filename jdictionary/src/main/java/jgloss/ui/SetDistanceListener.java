package jgloss.ui;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.logging.Level;

class SetDistanceListener extends TextChangeDocumentListener {

    public static final DecimalFormat DISTANCE_FORMAT = initDistanceFormat();

	private static DecimalFormat initDistanceFormat() {
        DecimalFormat format = new DecimalFormat("##0");
        format.setMaximumFractionDigits(0);
        format.setMaximumIntegerDigits(3);
		return format;
    }
	
	private final LookupModel model;
	
	SetDistanceListener(LookupModel model) {
		this.model = model;
	}
	
	@Override
    protected void textChanged(String text) {
        try {
            model.setDistance(DISTANCE_FORMAT.parse(text).intValue());
        } catch (ParseException ex) {
            LookupConfigPanel.LOGGER.log(Level.SEVERE, "failed to parse distance text as integer", ex);
        }
    }
	
}