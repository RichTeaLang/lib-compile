package richTea.compile;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public class BootstrapMain {
	public static final String PROGRAM_FILE_NAME = "program.tea";
	public static final Attributes.Name MODULES_ATTRIBUTE = new Attributes.Name("modules");
	public static final String MODULES_SEPARATOR = ";";

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		String selfPath = BootstrapMain.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		ClassLoader classLoader = new BootstrapClassLoader(new JarFile(selfPath));
		InputStream embeddedProgram = classLoader.getResourceAsStream(PROGRAM_FILE_NAME);
		Class<?> mainClass = classLoader.loadClass("richTea.runtime.RuntimeMain");
		Method runtime = mainClass.getMethod("main", InputStream.class, String[].class);
		
		runtime.invoke(null, embeddedProgram, args);
	}
}
