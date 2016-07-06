package richTea.compile.exports;

import richTea.compile.CompilationOutputStream;
import richTea.compiler.CompilationResult;
import richTea.runtime.execution.AbstractFunction;

public class BuildDistributable extends AbstractFunction {

	@Override
	protected void run() throws Exception {
		CompilationResult compliation = getCompliationResult();
		CompilationOutputStream output = new CompilationOutputStream(compliation);
		
		context.setLastReturnValue(output);
	}
	
	protected CompilationResult getCompliationResult() {
		return (CompilationResult) context.getValue("compilation");
	}
}
