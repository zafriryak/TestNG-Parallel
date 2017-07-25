package il.co.topq.difido;


import org.testng.*;
import org.testng.internal.IResultListener2;

import infra.parallel.listeners.TestIterationListener;
import infra.parallel.listeners.testCasesListener;






public class ReportManagerHook implements IResultListener2, ISuiteListener,IInvokedMethodListener2,testCasesListener,TestIterationListener{

	/**
	 * From some reason, TestNG is calling the suite events number of times.
	 * From that reason, we are keeping a counter to know when to close the
	 * execution. Also, from another strange reason, TestNG is registering the
	 * hook for every class that has the annotation. From this reason, it is
	 * important to keep the counter static
	 **/

	private static ReportManager reporterInstance = (ReportManager) ReportManager.getInstance();

	@Override
	public void onTestStart(ITestResult result) {
		reporterInstance.onTestStart(result);
	}

	@Override
	public void onTestSuccess(ITestResult result) {
		reporterInstance.onTestSuccess(result);
	}

	@Override
	public void onTestFailure(ITestResult result) {
		reporterInstance.onTestFailure(result);
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		reporterInstance.onTestSkipped(result);
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		
	}

	@Override
	public void onStart(ITestContext context) {
		reporterInstance.onStart(context);
	}

	@Override
	public void onFinish(ITestContext context) {
		reporterInstance.onFinish(context);
	}

	@Override
	public void onConfigurationSuccess(ITestResult result) {
		reporterInstance.onConfigurationSuccess(result);
	}

	@Override
	public void onConfigurationFailure(ITestResult result) {
		reporterInstance.onConfigurationFailure(result);
	}

	@Override
	public void onConfigurationSkip(ITestResult result) {
		reporterInstance.onConfigurationSkip(result);
	}

	@Override
	public void beforeConfiguration(ITestResult result) {
		reporterInstance.beforeConfiguration(result);
	}

	@Override
	public void onStart(ISuite suite) {
		reporterInstance.onStart(suite);
	}

	@Override
	public void onFinish(ISuite suite) {
		reporterInstance.onFinish(suite);
	}

	@Override
	public void validateTestMethodNode(String testWave, String testClass, String testMethod,Exception e) {
		reporterInstance.validateTestMethodNode(testWave,testClass,testMethod,e);
	}

	@Override
	public void validateTestClassNode(String testWaveXmlNode,
			String testClassXmlNode, Exception e) {
		reporterInstance.validateTestClassNode(testWaveXmlNode,testClassXmlNode,e);
		
	}

	@Override
	public void validateTestNGMethodSignature(String testWaveXmlNode,
			String testClassXmlNode, String testMethodXmlNode, Exception e) {
		reporterInstance.validateTestNGMethodSignature(testWaveXmlNode,testClassXmlNode,testMethodXmlNode,e);
		
	}

	@Override
	public void validateTestMandatoryParamsAlert(String testWaveXmlNode,
			String testClassXmlNode, String testMethodXmlNode, Exception e) {
		reporterInstance.validateTestMandatoryParamsAlert(testWaveXmlNode,testClassXmlNode,testMethodXmlNode,e);
	}

	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult, ITestContext context) {
		reporterInstance.beforeInvocation(method,testResult,context);
	}

	@Override
	public void afterInvocation(IInvokedMethod method, ITestResult testResult, ITestContext context) {
		reporterInstance.afterInvocation(method,testResult,context);
	}

	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
		reporterInstance.beforeInvocation(method,testResult);
	}

	@Override
	public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
		reporterInstance.afterInvocation(method,testResult);
	}
}
