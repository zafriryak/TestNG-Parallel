package infra.parallel.jaxb;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.QName;
import org.testng.ITestNGMethod;

import il.co.topq.difido.ReportManagerHook;
import infra.parallel.listeners.TestIterationListener;

public class Iteration {

	private Map<QName, Object> any;

	@XmlTransient private String sut;

	private String testName;

	private String testDescription;

	private boolean isEmptyIteration = false;

	public Iteration() {

	}

	@XmlAnyAttribute
	public Map<QName, Object> getAny() {
		if (any == null) {
			any = new HashMap<QName, Object>();
		}
		return any;
	}

	private TestIterationListener testIterationListener = new TestIterationListener() {

		@Override
		public void validateTestMandatoryParamsAlert(String testWaveXmlNode,
				String testClassXmlNode, String testMethodXmlNode,
				Exception e) {

		}

	};

	public Object getParamValue(String paramName) {
		return any.entrySet().stream()
				.filter(param -> param.getKey().getLocalPart()
						.equalsIgnoreCase(paramName))
				.findFirst().get().getValue();
	}

	public boolean containsParameter(String paramName) {
		if (isEmptyIteration) return false;
		return any.entrySet().stream().anyMatch(param -> param.getKey()
				.getLocalPart().equalsIgnoreCase(paramName));
	}

	public boolean containsMandatoryParams(String[] mandatoryParamsArray,
			ITestNGMethod method) {
	
		testIterationListener = new ReportManagerHook();
		String testWaveName = method.getXmlTest().getName();
		String testClassName = method.getTestClass().getRealClass()
				.getSimpleName();
		String testMethodName = method.getConstructorOrMethod().getMethod()
				.getName();
		
		if (getSut() == null) {
			String exceptionMessage = "(TestCases.xml error: Sut parent xml node doesnt exist, every iteration node must have a parent node (Sut name=...)\n"
					+ "at Class: " + testClassName + "\n at Method: "
					+ testMethodName;
			testIterationListener.validateTestMandatoryParamsAlert(testWaveName,
					testClassName, testMethodName,
					new Exception(exceptionMessage));
			System.err.println(exceptionMessage);
			return true;
		}
		boolean isAMandatoryParamMissing = false;
		if (mandatoryParamsArray.length > 0) {
			String[] mandatoryParamsSplitted = null;
			try {
				mandatoryParamsSplitted = mandatoryParamsArray[0].split(",");
			}
			catch (NullPointerException e) {
				System.err.println(e);
				return false;
			}
			StringBuilder missingParameters = new StringBuilder();
			for (String param : mandatoryParamsSplitted) {
				if (!this.containsParameter(param.replaceAll("\\s", ""))) {
					missingParameters.append(param.replaceAll("\\s", ""));
					isAMandatoryParamMissing = true;
				}
			}
			if (isAMandatoryParamMissing) {
			String exceptionMessage = "TestCases.xml / Mandatory Parameters error: the parameter/s "
					+ missingParameters.toString()
					+ " is/are missing from the TestCases.Xml file. \n"
					+ "The following test method contains a @MandatoryParam restriction for a specific params that must exist in every iteration in the method: \n"
					+ testMethodName + " in class: " + testClassName
					+ "please follow the MandatoryParams annotation params array, indicating on must to have params in every test iteration\n";
			testIterationListener.validateTestMandatoryParamsAlert(testWaveName,
					testClassName, testMethodName,
					new Exception(exceptionMessage));
			System.err.println(exceptionMessage);
			}
		} else {
			isEmptyIteration = true;
		}
		return isAMandatoryParamMissing;
	}

	public String getSut() {
		return sut;
	}

	public void setSut(String sut) {
		this.sut = sut;
	}

	public String getTestName() {
		return testName;
	}

	public void setTestName(String testName) {
		this.testName = testName;
	}

	@Override
	public final String toString() {
		return this.getTestName();
	}

	public String getTestDescription() {
		return testDescription;
	}

	public void setTestDescription(String testDescription) {
		this.testDescription = testDescription;

	}

}
