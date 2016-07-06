package richTea.compile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import richTea.compiler.CompilationResult;
import richTea.compiler.bootstrap.ImportNode;

public class CompilationOutputStream extends ByteArrayOutputStream {
	private CompilationResult compilation;
	private Set<String> dependencies;
	
	public CompilationOutputStream(CompilationResult compilation) throws IOException {
		this.compilation = compilation;
		this.dependencies = new HashSet<>(Arrays.asList(
			"richtea-runtime.jar",
			"commons-beanutils-1.9.2.jar",
			"log4j-1.2.17.jar",
			"antlr-runtime-3.5.2.jar",
			"commons-logging-1.1.1.jar",
			"commons-collections-3.2.1.jar"
		));
		
		Manifest manifest = createManifest(compilation.getImports());
		JarOutputStream output = new JarOutputStream(this, manifest);
		
		writeProgram(compilation.getSource(), output);
		writeImports(compilation.getImports(), output);
		writeRichTeaDependencies(output);
		writeBootstrapClassLoader(output);
		
		output.close();
	}

	public CompilationResult getCompilation() {
		return compilation;
	}
	
	protected Manifest createManifest(List<ImportNode> imports) {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		List<String> modulePaths = new ArrayList<>(imports.size());
		
		for(ImportNode node : imports) {
			modulePaths.add(node.getDeclaredPath().toString());
		}
		
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		attributes.put(Attributes.Name.MAIN_CLASS, BootstrapMain.class.getName());
		attributes.put(BootstrapMain.MODULES_ATTRIBUTE, String.join(BootstrapMain.MODULES_SEPARATOR, modulePaths));
		
		return manifest;
	}
	
	protected void writeProgram(String source, JarOutputStream output) throws IOException {
		output.putNextEntry(new ZipEntry(BootstrapMain.PROGRAM_FILE_NAME));
		output.write(source.getBytes(StandardCharsets.UTF_8));
		output.closeEntry();
	}
	
	protected void writeImports(List<ImportNode> imports, JarOutputStream output) throws IOException {
		Set<Path> importedPaths = new HashSet<>();
		
		for(ImportNode node : imports) {
			Path declaredPath = node.getDeclaredPath();
			Path absolutePath = node.getModulePath();
			
			if (importedPaths.add(declaredPath)) {
				StreamUtils.writeEntryToJar(declaredPath.toString(), new FileInputStream(absolutePath.toFile()), output);
			}
		}
	}
	
	protected void writeRichTeaDependencies(JarOutputStream output) throws FileNotFoundException, IOException {
		List<URL> urls = new ArrayList<>();
		ClassLoader classLoader = getClass().getClassLoader();
		
		while(classLoader != null) {
			if (classLoader instanceof URLClassLoader) {
				urls.addAll(Arrays.asList(((URLClassLoader) classLoader).getURLs()));
			}
			
			classLoader = classLoader.getParent();
		}
		
		for(URL url : urls) {
			try {
				File file = new File(url.toURI());
				
				if (file.isFile() && dependencies.contains(file.getName())) {
					writeFileDependency(file, output);
				} else if(file.isDirectory()) {
					writeDirectoryDependency(file, output);
				}
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	protected void writeFileDependency(File file, JarOutputStream output) throws FileNotFoundException, IOException {
		StreamUtils.writeEntryToJar(file.getName(), new FileInputStream(file), output);
	}
	
	protected void writeDirectoryDependency(File directory, JarOutputStream output) throws IOException {
		ByteArrayOutputStream dependencyBytes = new ByteArrayOutputStream();
		JarOutputStream dependencyJar = new JarOutputStream(dependencyBytes);
		Stack<File> stack = new Stack<>();
		
		stack.push(directory);
		
		while (!stack.isEmpty()) {
			File file = stack.pop();
			
			if (file.isFile()) {
				String fileName = directory.toURI().relativize(file.toURI()).getPath();
				FileInputStream in = new FileInputStream(file);
				StreamUtils.writeEntryToJar(fileName, in, dependencyJar);
			} else if (file.exists()) {
				for (File nestedFile : file.listFiles()) {
					stack.push(nestedFile);
				}
			}
		}
		
		dependencyJar.close();
		
		String dependencyName = "richtea-runtime.jar";
		InputStream dependency = new ByteArrayInputStream(dependencyBytes.toByteArray());
		StreamUtils.writeEntryToJar(dependencyName, dependency, output);
	}
	
	protected void writeBootstrapClassLoader(JarOutputStream output) throws IOException {
		writeClass(BootstrapMain.class, output);
		writeClass(BootstrapClassLoader.class, output);
		writeClass(StreamUtils.class, output);
	}
	
	protected void writeClass(Class<?> clazz, JarOutputStream output) throws IOException {
		String classPath = clazz.getName().replace(".", File.separator) + ".class";
		InputStream input = getClass().getClassLoader().getResourceAsStream(classPath);
		StreamUtils.writeEntryToJar(classPath, input, output);
	}
}
