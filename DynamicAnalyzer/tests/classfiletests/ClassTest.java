package classfiletests;

import classfiletests.utils.ClassRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import utils.logging.L1Logger;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;



@RunWith(Parameterized.class)
public class ClassTest{
    
	private final String name;
	private final boolean hasIllegalFlow;
	private final String[] involvedVars;
    

	/**
	 * @param name
	 * @param hasIllegalFlow
	 */
	public ClassTest(String name, boolean hasIllegalFlow, String... involvedVars) {
    
		this.name = name;
		this.hasIllegalFlow = hasIllegalFlow;
		this.involvedVars = involvedVars;
	}
    
	Logger logger = L1Logger.getLogger();
	
	@Parameters(name = "Name: {0}")
	public static Iterable<Object[]> generateParameters() {
		return Arrays.asList(
				new Object[] { "AccessFieldsOfObjectsFail", true, new String[] {}}, 
				new Object[] { "AccessFieldsOfObjectsSuccess",false,new String[]{}},
				new Object[] { "ArithmeticExpressionsFail", true, new String[] {}},
				new Object[] { "ArithmeticExpressionsSuccess",false,new String[]{}},
				new Object[] { "ArrayRef", true, new String[] {}},
				new Object[] { "ExtClasses", true, new String[] {}}, 
				new Object[] { "FieldsSuccess", false, new String[] {}},
				new Object[] { "FieldWriteFail", true, new String[] {}}, 
				new Object[] { "ForLoop", true, new String[] {}},
				new Object[] { "IfStmt", true, new String[] {}}, 
				new Object[] { "InvokeInternalMethod", true, new String[] {}}, 
				new Object[] { "InvokeLibMethod", true, new String[] {}},
				new Object[] { "MakeHigh", true, new String[] {}},
				new Object[] { "MulArray", true, new String[] {}}, 
				new Object[] { "NonStaticMethods", true, new String[] {}}, 
				new Object[] { "Simple", true, new String[]{"java.lang.String_r1"}},
				new Object[] { "StaticMethods", true, new String[] {}}, 
				new Object[] { "SwitchStmt", false, new String[] {}},
				new Object[] { "SwitchStmtFail", true, new String[] {}},
				new Object[] { "WhileLoop", false, new String[] {}},
				new Object[] { "WhileLoopFail", true, new String[] {}});
	}

	@Test
	public void test() {
		System.out.println("\n\n\n");
		logger.info("Start of executing main.testclasses." + name + "");

		ClassRunner.testClass(name, hasIllegalFlow);

		logger.info("Finished executing main.testclasses." + name + "");
	}
}
