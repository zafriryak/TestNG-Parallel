package infra.parallel.listeners;

public interface TestIterationListener {
	public void validateTestMandatoryParamsAlert(String testWaveXmlNode,String testClassXmlNode, String testMethodXmlNode,Exception e);
}
