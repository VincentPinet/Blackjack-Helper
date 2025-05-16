package com.github.vincentpinet.blackjack_helper;

import java.util.HashMap;

public class Kelly {

	private static final double EPS = 0.00001;
	private static final double x0 = 0.05;

	public static final double criterion(final HashMap<Double, Double> distribution) {

		// don't play if -ev
		if (distribution.entrySet()
			.stream()
			.mapToDouble( e -> e.getKey() * e.getValue())
			.sum() < 0)
				return 0;

		// play +inf if can't lose
		if (distribution.keySet()
			.stream()
			.min(Double::compare)
			.get() > 0)
				return Double.POSITIVE_INFINITY;

		// maximize f(x) = sum_i(pi*log(1+bi*x))
		// Newton's method
		double xt, xtplus1 = x0;
		do {

			xt = xtplus1;
			xtplus1 = xt - fprime(xt, distribution) / fdoubleprime(xt, distribution);

		} while (Math.abs(xtplus1 - xt) > EPS);

		return xtplus1;
	}

	// f'(x) = sum_i(pi*bi/(1+bi*x))
	private static final double fprime(double x, HashMap<Double, Double> distribution) {
		return distribution.entrySet()
			.stream()
			.mapToDouble(e -> e.getValue() * e.getKey() / (1 + e.getKey() * x))
			.sum();
	}

	// f''(x) = sum_i(-pi*bi^2/(1+bi*x)^2)
	private static final double fdoubleprime(double x, HashMap<Double, Double> distribution) {
		return distribution.entrySet()
			.stream()
			.mapToDouble(e -> -e.getValue() * Math.pow(e.getKey(), 2)/ (Math.pow(1 + e.getKey() * x, 2)))
			.sum();
	}
}
