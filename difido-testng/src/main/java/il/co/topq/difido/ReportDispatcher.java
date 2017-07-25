package il.co.topq.difido;

import java.io.File;

import il.co.topq.difido.model.Enums.Status;
import org.testng.IInvokedMethod;
import org.testng.ITestContext;
import org.testng.ITestResult;

public interface ReportDispatcher {

	public abstract void logHtml(String title,Status status);

	public abstract void logHtml(String title, String message, Status status);

	public abstract void log(String title);

	public abstract void log(String title, Status status);

	public abstract void log(String title, String message);

	public abstract void log(String title, String message, Status status);

	public abstract void startLevel(String description);

	public abstract void endLevel();

	public abstract void step(String description);

	public abstract void addFile(File file, String description);

	public abstract void addImage(File file, String description);

	public abstract void addLink(String link, String description);
	
	public abstract void addTestProperty(String name, String value);
	
	public abstract void addRunProperty(String name, String value);

	String getCurrentTestFolder();
	
	Status getCurrentTestStatus();

	public void beforeInvocation(IInvokedMethod method, ITestResult testResult, ITestContext context);

	public void afterInvocation(IInvokedMethod method, ITestResult testResult, ITestContext context);

	public void beforeInvocation(IInvokedMethod method, ITestResult testResult);

	public void afterInvocation(IInvokedMethod method, ITestResult testResult);
}