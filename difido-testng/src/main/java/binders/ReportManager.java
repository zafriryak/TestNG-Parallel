package binders;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import il.co.topq.difido.ReportDispatcher;



public class ReportManager {

	private static class ReportInvocationHandler implements InvocationHandler {
		private ReportDispatcher object;

		public ReportInvocationHandler(ReportDispatcher object) {
			this.object = object;
		}

		@Override
		public Object invoke(Object originalObject, Method method, Object[] argumentsToMethod) throws Throwable {
			return method.invoke(object, argumentsToMethod);
		}
	}

	public static IReporter getInstance() {
		ReportInvocationHandler handler = new ReportInvocationHandler(il.co.topq.difido.ReportManager.getInstance());
		Class<?>[] aaa = { IReporter.class };
		return (IReporter) Proxy.newProxyInstance(IReporter.class.getClassLoader(), aaa, handler);
	}
}
