package il.co.topq.difido;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * This entity decides how many times a test needs to be rerun. TestNg will call
 * the retry method every time a test fails. So we can put some code in here to
 * decide when to rerun the test.
 * 
 * Note: the retry method will return true if a tests needs to be retried and
 * false it not.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

	private static AtomicInteger counter = new AtomicInteger(0);
	private final static int MAX_RETRY_COUNT = 1;
	private static Set<String> retryTestList;

	static {
		retryTestList = new ConcurrentHashSet<>();
	}

	@Override
	public boolean retry(ITestResult result) {
		if (retryTestList.contains(result.getTestName()))
			return false;
		if (counter.get() < MAX_RETRY_COUNT) {
			retryTestList.add(result.getTestName());
			counter.getAndIncrement();
			return true;
		}
		return false;
	}

	public int getCounter() {
		return counter.get();
	}
	
	public boolean isParentNode(){
		return counter.get()==0;
	}
	
	public int getMaxRetryCount(){
		return MAX_RETRY_COUNT;
	}

}
