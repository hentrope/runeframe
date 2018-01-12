package netscape.javascript;

import java.applet.Applet;

public class JSObject {
	private static final JSObject obj = new JSObject();

	public static JSObject getWindow(Applet applet) {
		return obj;
	}



	public Object call(String methodName, Object[] args)	{
		switch (methodName) {
		case "resize":
			break;
		case "zap":
		case "unzap":
			//System.out.println("Zap!");
			break;
		case "loggedout":
			//System.out.println("Logged out.");
			break;
		default:
			System.out.print(methodName + "(");

			if (args != null) for (int i = 0; i < args.length; i++) {
				System.out.print(args[i] == null ? "null" : args[i].toString());

				if (i < args.length - 1)
					System.out.print(", ");
			}

			System.out.println(")");
		}

		return null;
	}

	public Object eval(String s) {
		System.out.println("eval(" + s + ")");
		return null;
	}
}
