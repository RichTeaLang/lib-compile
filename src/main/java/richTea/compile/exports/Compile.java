package richTea.compile.exports;

import richTea.compiler.CompilationResult;
import richTea.compiler.Compiler;
import richTea.runtime.execution.AbstractFunction;

public class Compile extends AbstractFunction {

	@Override
	protected void run() throws Exception {
		String source = getSource();
		Compiler compiler = new Compiler(source);
		CompilationResult compilation = compiler.compile();
		
		context.setLastReturnValue(compilation);
	}
	
	protected String getSource() {
		return context.getString("source");
	}
}
