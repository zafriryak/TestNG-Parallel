package infra.parallel.jaxb;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.testng.ITestNGMethod;

import il.co.topq.difido.ReportManagerHook;
import infra.parallel.listeners.testCasesListener;

@XmlRootElement
public class Scenarios {

	private List<TestClass> testclass;

	public Scenarios(List<TestClass> testclass) {
		this();
		this.setTestclass(testclass);
	}

	public Scenarios() {
		testCasesListener = new ReportManagerHook();
	}

	private testCasesListener testCasesListener = new testCasesListener() {
		@Override
		public void validateTestMethodNode(String testWave, String testClass,
				String testMethod, Exception e) {

		}

		@Override
		public void validateTestClassNode(String testWaveXmlNode,
				String testClassXmlNode, Exception e) {

		}

		@Override
		public void validateTestNGMethodSignature(String testWaveXmlNode,
				String testClassXmlNode, String testMethodXmlNode,
				Exception e) {

		}

	};

	@XmlElement
	public List<TestClass> getTestclass() {
		return testclass;
	}

	public List<Iteration> getTestMethodIterations(String testMethodName,
			String testMethodClassName) {
		TestClass tc = getTestClassByName(testMethodClassName);
		return tc.getIterationsByMethod(testMethodName);
	}

	private int getTestMethodIterationsCount(ITestNGMethod iTestNGMethod)
			throws Exception {
		TestClass tc = getTestClassByName(
				iTestNGMethod.getTestClass().getRealClass().getSimpleName());
		return tc.getIterationsByMethod(
				iTestNGMethod.getConstructorOrMethod().getMethod().getName())
				.size();
	}

	public int getSuiteIterationsCount(ITestNGMethod[] testMethods)
			throws Exception {
		int iterationsSum = 0;
		for (ITestNGMethod method : testMethods) {
			iterationsSum += getTestMethodIterationsCount(method);
		}
		return iterationsSum;
	}

	private TestClass getTestClassByName(String testMethodClassName) {
		Optional<TestClass> findFirst = testclass.stream()
				.filter(tc -> tc.getName().equals(testMethodClassName))
				.findFirst();
		if (!findFirst.isPresent()) {
			try {
				throw new NullPointerException(
						"TestClass: " + testMethodClassName + " doesnt exist");
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return findFirst.get();
	}

	public void testMethodExistInTestCasesXML(ITestNGMethod iTestNGMethod)
			throws Exception {

		String testNgXmlTestWaveNodeName = iTestNGMethod.getXmlTest().getName();
		String testNgXmlClassNodeName = iTestNGMethod.getTestClass().getName()
				.substring(
						iTestNGMethod.getTestClass().getName().lastIndexOf(".")
								+ 1);
		String testNgXmlMethodNodeName = iTestNGMethod.getConstructorOrMethod()
				.getMethod().getName();

		final String testMethodMissingExcep = "TestCases.xml exception: TestMethod Node is missing,  TestMethod: *"
				+ testNgXmlMethodNodeName + "* \nin the class: "
				+ testNgXmlClassNodeName + "\nFrom the TestCases file at: ("
				+ iTestNGMethod.getTestClass().getXmlTest().getSuite()
						.getParameter("TestCasesFilePath")
				+ ")"
				+ " \nthe parallel testng framework must have a context for the referenced testmethod & testclass in order to execute.";
		final String testClassMissingExcep = "TestCases.xml exception:  TestClass: *"
				+ testNgXmlClassNodeName + "* is missing "
				+ "\nFrom the TestCases.xml file at: ("
				+ iTestNGMethod.getTestClass().getXmlTest().getSuite()
						.getParameter("TestCasesFilePath")
				+ ")"
				+ " \nthe parallel testng framework must have a context for the referenced testmethod & testclass in order to execute.";

		try {
			// getTestClassByName throws a misleading exception which is the
			// reason it isn't used in this method
			testclass.stream()
					.filter(tc -> tc.getName().equals(iTestNGMethod
							.getTestClass().getRealClass().getSimpleName()))
					.findFirst().get();
		}
		catch (Exception e1) {
			testCasesListener.validateTestClassNode(testNgXmlTestWaveNodeName,
					testNgXmlClassNodeName,
					new Exception(testClassMissingExcep));
			System.err.println(testClassMissingExcep);
		}
		try {
			testclass.stream()
					.filter(tc -> tc.getName()
							.equals(iTestNGMethod.getTestClass().getRealClass()
									.getSimpleName()))
					.findFirst()
					.get().getTestMethodByName(iTestNGMethod
							.getConstructorOrMethod().getMethod().getName())
					.get();
		}
		catch (Exception e2) {
			testCasesListener.validateTestMethodNode(testNgXmlTestWaveNodeName,
					testNgXmlClassNodeName, testNgXmlMethodNodeName,
					new Exception(testMethodMissingExcep));
			System.err.println(testMethodMissingExcep);
		}
	}

	public void testMethodSignatureValidation(ITestNGMethod method)
			throws Exception {
		String testNgXmlTestWaveNodeName = method.getXmlTest().getName();
		String testNgXmlClassNodeName = method.getTestClass().getName()
				.substring(
						method.getTestClass().getName().lastIndexOf(".") + 1);
		String testNgXmlMethodNodeName = method.getConstructorOrMethod()
				.getMethod().getName();

		String testMethodExcep = "";

		if (!Arrays
				.stream(method.getConstructorOrMethod().getMethod()
						.getGenericParameterTypes())
				.collect(Collectors.toList()).contains(Iteration.class)) {
			testMethodExcep = "Parallel framework exception:  wrong method signature!"
					+ "test method: "
					+ method.getConstructorOrMethod().getMethod().getName()
					+ " must contain an Iteration argument, in class: "
					+ method.getRealClass().getSimpleName();
			System.err.println(testMethodExcep);
			testCasesListener.validateTestNGMethodSignature(
					testNgXmlTestWaveNodeName, testNgXmlClassNodeName,
					testNgXmlMethodNodeName, new Exception(testMethodExcep));
			throw new Exception(testMethodExcep);
		}
	}

	public void setTestclass(List<TestClass> testclass) {
		this.testclass = testclass;
	}



}
