package main.testclasses;

import utils.analyzer.HelperClass;


public class SimpleDebug {
	public static void main(String[] args) {
		
		C b = new C();
		C c = HelperClass.makeHigh(b);
		System.out.println(c);
		
	}
}
