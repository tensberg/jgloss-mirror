package jgloss.ui;

class SetSearchExpressionListener extends TextChangeDocumentListener {
	private final LookupModel model;

	SetSearchExpressionListener(LookupModel model) {
		this.model = model;
	}

	@Override
    protected void textChanged(String text) {
        model.setSearchExpression(text);
    }
}