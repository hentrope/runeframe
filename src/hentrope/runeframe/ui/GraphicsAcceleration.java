package hentrope.runeframe.ui;

/**
 * Class containing a <code>set</code> method, which is used to change
 * the graphics acceleration method at startup time.
 * 
 * @author hentrope
 */
public class GraphicsAcceleration {
	public static void set(String value) {
		switch (value.toUpperCase()) {
		case "OPENGL":
			System.setProperty("sun.java2d.opengl", "true");
			System.setProperty("sun.java2d.noddraw", "true");
			return;
		case "DIRECTDRAW":
			System.setProperty("sun.java2d.d3d", "false");
			return;
		case "DIRECT3D":
			return;
		case "XRENDER":
			System.setProperty("sun.java2d.xrender", "true");
			System.setProperty("sun.java2d.noddraw", "true");
			return;
		default:
		case "SOFTWARE":
			System.setProperty("sun.java2d.noddraw", "true");
			return;
		}
	}
}
