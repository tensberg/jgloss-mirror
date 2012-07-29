package jgloss.ui;

import static java.util.Collections.unmodifiableList;

import java.awt.Image;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;

public class JGlossLogo {
	public static final ImageIcon LOGO_LARGE = getLogo("");
	
	public static final ImageIcon LOGO_64 = getLogo("_64");
	
	public static final ImageIcon LOGO_48 = getLogo("_48");
	
	public static final ImageIcon LOGO_32 = getLogo("_32");
	
	public static final List<Image> ALL_LOGO_SIZES = unmodifiableList(Arrays.asList(new Image[] { LOGO_LARGE.getImage(), LOGO_64.getImage(), LOGO_48.getImage(), LOGO_32.getImage() }));
	
	private static ImageIcon getLogo(String sizeExtension) {
		return new ImageIcon(JGlossLogo.class.getResource("/images/jgloss-logo" + sizeExtension + ".png"));
	}
	
	private JGlossLogo() {
	}
}
