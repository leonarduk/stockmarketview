//package com.leonarduk.finance.stockfeed.interpolation;
//
//import java.util.Arrays;
//import java.util.List;
//
//import java.time.LocalDate;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//
//import eu.verdelhan.ta4j.Decimal;
//import eu.verdelhan.ta4j.Tick;
//import eu.verdelhan.ta4j.TimeSeries;
//
//public class LinearInterpolatorTest {
//	private TimeSeriesInterpolator	interpolator;
//	private TimeSeries				series;
//
//	@Before
//	public void setUp() throws Exception {
//		this.interpolator = new LinearInterpolator();
//		final List<Tick> ticks = Arrays.asList(new Tick[] { //
//		        new Tick(LocalDate.parse("2017-04-14").toDateTimeAtStartOfDay(),
//		                105, 115, 95, 110, 2000),
//		        new Tick(LocalDate.parse("2017-04-07").toDateTimeAtStartOfDay(),
//		                100, 112, 92, 102, 5000),
//		        new Tick(LocalDate.parse("2017-04-03").toDateTimeAtStartOfDay(),
//		                100, 110, 90, 105, 1000) });
//		this.series = new TimeSeries(ticks);
//	}
//
//	@Test
//	public void testInterpolate() {
//		final TimeSeries actual = this.interpolator.interpolate(this.series);
//		Assert.assertEquals(10, actual.getTickCount());
//		Assert.assertEquals(LocalDate.parse("2017-04-03"),
//		        actual.getTick(0).getEndTime().toLocalDate());
//		Assert.assertEquals(LocalDate.parse("2017-04-04"),
//		        actual.getTick(1).getEndTime().toLocalDate());
//		Assert.assertEquals(LocalDate.parse("2017-04-05"),
//		        actual.getTick(2).getEndTime().toLocalDate());
//		Assert.assertEquals(LocalDate.parse("2017-04-07"),
//		        actual.getTick(4).getEndTime().toLocalDate());
//		Assert.assertEquals(LocalDate.parse("2017-04-14"),
//		        actual.getTick(9).getEndTime().toLocalDate());
//
//		Assert.assertEquals(Decimal.valueOf(102.75),
//		        actual.getTick(3).getClosePrice());
//		Assert.assertEquals(Decimal.valueOf(103.5),
//		        actual.getTick(2).getClosePrice());
//		Assert.assertEquals(Decimal.valueOf(104.25),
//		        actual.getTick(1).getClosePrice());
//		Assert.assertEquals(Decimal.valueOf(102),
//		        actual.getTick(4).getClosePrice());
//		Assert.assertEquals(Decimal.valueOf(106.8).toDouble(),
//		        actual.getTick(5).getClosePrice().toDouble(), 0.001);
//
//	}
//
//}
