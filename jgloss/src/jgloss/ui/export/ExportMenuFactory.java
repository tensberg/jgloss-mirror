package jgloss.ui.export;

public class ExportMenu extends JMenu implements ActionListener {
    private static List exporters = new ArrayList();
    
    public static synchronized void registerExporter( InputSource in) {
        exporters.add( new Exporter( in));
    }
} // class ExportMenu
