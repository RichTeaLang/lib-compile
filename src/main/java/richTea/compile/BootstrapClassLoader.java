package richTea.compile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class BootstrapClassLoader extends URLClassLoader {
	private JarFile jarFile;
	
	public BootstrapClassLoader(JarFile jarFile) throws IOException {
		super(new URL[] {});
		
		this.jarFile = jarFile;
		
		Set<String> modulePaths = getModulePaths(this.jarFile);
		Map<String, File> dependencies = extractJarDependencies(this.jarFile);
		
		for(Map.Entry<String, File> entry : dependencies.entrySet()) {
			if (modulePaths.contains(entry.getKey())) {
				// Don't add modules to the class path.  Instead add the path of the extracted module to system
				// properties - ImportNode instances will handle loading of the modules.
				System.setProperty(entry.getKey(), entry.getValue().getAbsolutePath());
			} else {
				addURL(entry.getValue().toURI().toURL());
			}
		}
	}
	
	public JarFile getJarFile() {
		return jarFile;
	}
	
	protected Set<String> getModulePaths(JarFile jarFile) throws IOException {
		String modulesAttribute = jarFile.getManifest().getMainAttributes().getValue(BootstrapMain.MODULES_ATTRIBUTE);
		
		return new HashSet<>(Arrays.asList(modulesAttribute.split(BootstrapMain.MODULES_SEPARATOR)));
	}
	
	protected Map<String, File> extractJarDependencies(JarFile jarFile) throws IOException {
		Map<String, File> dependencies = new HashMap<>();
		Enumeration<JarEntry> entries = jarFile.entries();
		String tempDir = System.getProperty("java.io.tmpdir");
		
		while(entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			
			if (entry.getName().endsWith(".jar")) {
				File destination = new File(tempDir, entry.getName());
				
				destination.getParentFile().mkdirs();
				destination.createNewFile();
				destination.deleteOnExit();
				
				InputStream input = jarFile.getInputStream(entry);
				FileOutputStream output = new FileOutputStream(destination);
				StreamUtils.pipe(input, output);
				
				input.close();
				output.close();
				
				dependencies.put(entry.getName(), destination);
			}
		}
		
		return dependencies;
	}
}
