/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2016 Marc de Verdelhan & respective authors (see AUTHORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.leonarduk.finance.analysis;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import com.leonarduk.finance.stockfeed.DailyTimeseries;
import com.leonarduk.finance.stockfeed.IntelligentStockFeed;
import com.leonarduk.finance.stockfeed.StockFeed;
import com.leonarduk.finance.stockfeed.StockFeed.EXCHANGE;
import com.leonarduk.finance.strategies.AbstractStrategy;
import com.leonarduk.finance.strategies.GlobalExtremaStrategy;
import com.leonarduk.finance.strategies.MovingMomentumStrategy;
import com.leonarduk.finance.strategies.SimpleMovingAverageStrategy;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Indicator;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.Trade;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import yahoofinance.Stock;

/**
 * This class builds a graphical chart showing the buy/sell signals of a
 * strategy.
 */
public class BuyAndSellSignalsToChart {
	public static void main(String[] args) throws IOException {

		// Getting the time series
		StockFeed feed = new IntelligentStockFeed();
		String ticker = "ISXF";
		Stock stock = feed.get(EXCHANGE.London, ticker, 1).get();
		TimeSeries series = DailyTimeseries.getTimeSeries(stock);

		// Building the trading strategy
		List<AbstractStrategy> strategies = new ArrayList<>();
		strategies.add(GlobalExtremaStrategy.buildStrategy(series));
		strategies.add(MovingMomentumStrategy.buildStrategy(series, 12, 26, 9));

		strategies.add(SimpleMovingAverageStrategy.buildStrategy(series, 12));
		strategies.add(SimpleMovingAverageStrategy.buildStrategy(series, 20));
		strategies.add(SimpleMovingAverageStrategy.buildStrategy(series, 50));

		displayBuyAndSellChart(series, strategies, stock.getName());
	}

	public static void displayBuyAndSellChart(TimeSeries series, List<AbstractStrategy> strategies, String name) {
		/**
		 * Building chart datasets
		 */
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		dataset.addSeries(buildChartTimeSeries(series, new ClosePriceIndicator(series), name));

		/**
		 * Creating the chart
		 */
		JFreeChart chart = ChartFactory.createTimeSeriesChart(name, // title
				"Date", // x-axis label
				"Price", // y-axis label
				dataset, // data
				true, // create legend?
				true, // generate tooltips?
				false // generate URLs?
		);
		XYPlot plot = (XYPlot) chart.getPlot();
		DateAxis axis = (DateAxis) plot.getDomainAxis();
		// axis.setDateFormatOverride(new SimpleDateFormat("MM-dd HH:mm"));

		/**
		 * Running the strategy and adding the buy and sell signals to plot
		 */
		for (AbstractStrategy strategy2 : strategies) {
			addBuySellSignals(series, strategy2, plot);
		}

		/**
		 * Displaying the chart
		 */
		displayChart(chart);
	}

	/**
	 * Builds a JFreeChart time series from a Ta4j time series and an indicator.
	 * 
	 * @param tickSeries
	 *            the ta4j time series
	 * @param indicator
	 *            the indicator
	 * @param name
	 *            the name of the chart time series
	 * @return the JFreeChart time series
	 */
	private static org.jfree.data.time.TimeSeries buildChartTimeSeries(TimeSeries tickSeries,
			Indicator<Decimal> indicator, String name) {
		org.jfree.data.time.TimeSeries chartTimeSeries = new org.jfree.data.time.TimeSeries(name);
		for (int i = 0; i < tickSeries.getTickCount(); i++) {
			Tick tick = tickSeries.getTick(i);
			chartTimeSeries.add(new Minute(tick.getEndTime().toDate()), indicator.getValue(i).toDouble());
		}
		return chartTimeSeries;
	}

	/**
	 * Runs a strategy over a time series and adds the value markers
	 * corresponding to buy/sell signals to the plot.
	 * 
	 * @param series
	 *            a time series
	 * @param strategy2
	 *            a trading strategy
	 * @param plot
	 *            the plot
	 */
	private static void addBuySellSignals(TimeSeries series, AbstractStrategy strategy2, XYPlot plot) {
		// Running the strategy
		List<Trade> trades = series.run(strategy2.getStrategy()).getTrades();
		// Adding markers to plot
		for (Trade trade : trades) {
			// Buy signal
			double buySignalTickTime = new Minute(series.getTick(trade.getEntry().getIndex()).getEndTime().toDate())
					.getFirstMillisecond();
			Marker buyMarker = new ValueMarker(buySignalTickTime);
			buyMarker.setPaint(Color.GREEN);
			buyMarker.setLabel("B");
			plot.addDomainMarker(buyMarker);
			// Sell signal
			double sellSignalTickTime = new Minute(series.getTick(trade.getExit().getIndex()).getEndTime().toDate())
					.getFirstMillisecond();
			Marker sellMarker = new ValueMarker(sellSignalTickTime);
			sellMarker.setPaint(Color.RED);
			sellMarker.setLabel("S");
			plot.addDomainMarker(sellMarker);
		}
	}

	/**
	 * Displays a chart in a frame.
	 * 
	 * @param chart
	 *            the chart to be displayed
	 */
	private static void displayChart(JFreeChart chart) {
		// Chart panel
		ChartPanel panel = new ChartPanel(chart);
		panel.setFillZoomRectangle(true);
		panel.setMouseWheelEnabled(true);
		panel.setPreferredSize(new Dimension(1024, 400));
		// Application frame
		ApplicationFrame frame = new ApplicationFrame("Ta4j example - Buy and sell signals to chart");
		frame.setContentPane(panel);
		frame.pack();
		RefineryUtilities.centerFrameOnScreen(frame);
		frame.setVisible(true);
	}

}