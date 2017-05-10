package com.leonarduk.finance.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.leonarduk.finance.stockfeed.IntelligentStockFeed;
import com.leonarduk.finance.stockfeed.Stock;
import com.leonarduk.finance.stockfeed.interpolation.LinearInterpolator;

import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import yahoofinance.histquotes.HistoricalQuote;

public class TimeseriesUtils {
	private static Double ensureIsDouble(final Number bigDecimal) {
		if (bigDecimal == null) {
			return null;
		}
		return bigDecimal.doubleValue();
	}

	public static Comparator<? super HistoricalQuote> getComparator() {
		final Comparator<? super HistoricalQuote> comparator = (o1, o2) -> {
			return o2.getDate().compareTo(o1.getDate());
		};
		return comparator;
	}

	public static HistoricalQuote getMostRecentQuote(final List<HistoricalQuote> history) {
		final HistoricalQuote firstQuote = history.get(0);
		return firstQuote;
	}

	public static HistoricalQuote getOldestQuote(final List<HistoricalQuote> history) {
		final HistoricalQuote lastQuote = history.get(history.size() - 1);
		return lastQuote;
	}

	public static TimeSeries getTimeSeries(final Stock stock, final int i) throws IOException {
		// TODO Auto-generated method stub
		return getTimeSeries(stock, LocalDate.now().minusYears(i), LocalDate.now());
	}

	public static TimeSeries getTimeSeries(final Stock stock, final LocalDate fromDate, final LocalDate toDate)
			throws IOException {
		List<HistoricalQuote> history = stock.getHistory();
		if ((null == history) || history.isEmpty()) {
			final Optional<Stock> optional = new IntelligentStockFeed().get(stock.getInstrument(), fromDate, toDate);
			if (optional.isPresent()) {
				history = optional.get().getHistory();
			} else {
				return null;
			}
		}

		sortQuoteList(history);
		final Iterator<HistoricalQuote> series = history.iterator();

		final List<Tick> ticks = new LinkedList<>();
		while (series.hasNext()) {
			try {
				final HistoricalQuote quote = series.next();

				final BigDecimal closeBd = quote.getClose();
				final double open = ensureIsDouble(ifNull(quote.getOpen(), closeBd));
				final double high = ensureIsDouble(ifNull(quote.getHigh(), closeBd));
				final double low = ensureIsDouble(ifNull(quote.getLow(), closeBd));
				final double close = ensureIsDouble(closeBd);
				final double volume = ensureIsDouble(ifNull(quote.getVolume(), 0L));

				ticks.add(new Tick(new DateTime(quote.getDate().toDateTimeAtStartOfDay()), open, high, low, close,
						volume));
			} catch (final NullPointerException e) {
				System.err.println(e);
				return null;
			}
		}
		return new LinearInterpolator().interpolate(new TimeSeries(stock.getName(), ticks));
	}

	public static Iterator<Tick> getTimeSeriesIterator(final TimeSeries series) {
		final Iterator<Tick> iter = new Iterator<Tick>() {
			int index = series.getEnd();

			@Override
			public boolean hasNext() {
				return this.index > (series.getBegin() - 1);
			}

			@Override
			public Tick next() {
				return series.getTick(this.index--);
			}
		};
		return iter;
	}

	private static Number ifNull(final Number open, final Number close) {
		if (open == null) {
			return close;
		}
		return open;
	}

	public static List<HistoricalQuote> sortQuoteList(final List<HistoricalQuote> history) {
		Collections.sort(history, getComparator());
		return history;
	}
}
