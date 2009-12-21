package br.com.gennex.test;

import junit.framework.Test;

public class TestAll {
	public static Test suite() {
		junit.framework.TestSuite suite = new junit.framework.TestSuite(
				"Framework Socket");
		suite.addTestSuite(FppsMessageTest.class);
		suite.addTestSuite(FppsTcpCommandSocketTest.class);
		suite.addTestSuite(HttpTcpCommandSocketTest.class);
		suite.addTestSuite(ServerNameTest.class);
		suite.addTestSuite(ServerPortTest.class);
		return suite;
	}
}