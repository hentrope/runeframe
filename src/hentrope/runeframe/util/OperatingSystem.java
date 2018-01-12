package hentrope.runeframe.util;

/**
 * Datatype describing the different operating systems the client may be run on.
 * 
 * @author hentrope
 */
public enum OperatingSystem {
	WINDOWS,
	MAC,
	LINUX,
	UNKNOWN;

	private static OperatingSystem current = null;

	/**
	 * Gets the operating system that the client is currently being run on.
	 * <p>
	 * If the system property "os.name" contains "win" or "mac", it will be
	 * considered Windows or Mac respectively. If it contains "nix", "nux", or
	 * "aix", it will be considered Linux. Otherwise, it is Unknown.
	 * 
	 * @return the current operating system
	 */
	public static OperatingSystem getCurrent() {
		if (current == null) {
			String name = System.getProperty("os.name");

			if (name != null) {
				name = name.toLowerCase();

				if (name.contains("win"))
					current = WINDOWS;
				else if (name.contains("mac"))
					current = MAC;
				else if (name.contains("nix") || name.contains("nux") || name.contains("aix"))
					current = LINUX;
				else
					current = UNKNOWN;
			} else
				current = UNKNOWN;
		}

		return current;
	}
}
