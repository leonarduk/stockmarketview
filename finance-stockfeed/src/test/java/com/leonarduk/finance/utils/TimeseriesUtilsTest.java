package com.leonarduk.finance.utils;

import java.math.BigDecimal;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.leonarduk.finance.stockfeed.Instrument;

import yahoofinance.histquotes.HistoricalQuote;

public class TimeseriesUtilsTest {

	private List<HistoricalQuote> getQuotes() {
		final List<HistoricalQuote> series = Lists.newArrayList();
		series.add(new HistoricalQuote(Instrument.CASH,
		        LocalDate.parse("2017-01-01"), BigDecimal.valueOf(12.3),
		        BigDecimal.TEN, BigDecimal.valueOf(9.3),
		        BigDecimal.valueOf(12.2), BigDecimal.valueOf(12.2), 23L,
		        "TestCache"));
		return series;
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public final void testSeriesToCsv() {
		final StringBuilder actual = TimeseriesUtils
		        .seriesToCsv(this.getQuotes());
		Assert.assertEquals(
		        "date,open,high,low,close,volume\n"
		                + "2017-01-01,12.30,9.30,10.00,12.20,23,TestCache\n",
		        actual.toString());
	}

}