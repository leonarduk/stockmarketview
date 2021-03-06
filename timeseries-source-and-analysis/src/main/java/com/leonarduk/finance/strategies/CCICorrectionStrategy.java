/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2016 Marc de Verdelhan & respective authors (see AUTHORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.leonarduk.finance.strategies;

import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.CCIIndicator;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

/**
 * CCI Correction Strategy
 *
 * http://stockcharts.com/school/doku.php?id=chart_school:trading_strategies:cci_correction
 */
public class CCICorrectionStrategy {

	public static Strategy buildStrategy(final TimeSeries series) {
		if (series == null) {
			throw new IllegalArgumentException("Series cannot be null");
		}

		final CCIIndicator longCci = new CCIIndicator(series, 200);
		final CCIIndicator shortCci = new CCIIndicator(series, 5);
		final Double plus100 = Double.valueOf(100);
		final Double minus100 = Double.valueOf(-100);

		final Rule entryRule = new OverIndicatorRule(longCci, plus100) // Bull
		                                                               // trend
		        .and(new UnderIndicatorRule(shortCci, minus100)); // Signal

		final Rule exitRule = new UnderIndicatorRule(longCci, minus100) // Bear
		                                                                // trend
		        .and(new OverIndicatorRule(shortCci, plus100)); // Signal

		final Strategy strategy = new BaseStrategy(entryRule, exitRule);
		strategy.setUnstablePeriod(5);
		return strategy;
	}

}
