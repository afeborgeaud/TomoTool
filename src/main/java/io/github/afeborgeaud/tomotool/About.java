package io.github.afeborgeaud.tomotool;

/**
 * Hello world!
 *
 */
public class About
{
	public static final String EMAIL = "aborgeaud@gmail.com";
	public static final String VERSION = "1.0";
	public static final String LINE = "TomoTool " + VERSION + "\n" +
			"Copyright \u00a9 2020-2021 Anselme Borgeaud.";
			
    public static void main( String[] args )
    {
        displayInfo();
    }
	
	private static void displayInfo() {
		System.out.println(LINE);
	}
}
