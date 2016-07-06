package richTea.compile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class StreamUtils {
	
	public static void pipe(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[2048];
		int bytesRead = -1;
		
		while((bytesRead = in.read(buffer)) != -1) {
			out.write(buffer, 0, bytesRead);
		}
		
		in.close();
	}
	
	public static void writeEntryToJar(String filename, InputStream in, JarOutputStream out) throws IOException {
		out.putNextEntry(new ZipEntry(filename));
		pipe(in, out);
		out.closeEntry();
	}
}
