package com.leonarduk.finance.stockfeed;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Bar;
import org.ta4j.core.num.DoubleNum;

import com.google.common.collect.Lists;
import com.leonarduk.finance.stockfeed.feed.ExtendedHistoricalQuote;
import com.leonarduk.finance.stockfeed.feed.yahoofinance.ExtendedStockQuote;
import com.leonarduk.finance.stockfeed.feed.yahoofinance.StockQuoteBuilder;
import com.leonarduk.finance.stockfeed.feed.yahoofinance.StockV1;
import com.leonarduk.finance.stockfeed.interpolation.FlatLineInterpolator;
import com.leonarduk.finance.utils.DateUtils;
import com.leonarduk.finance.utils.TimeseriesUtils;

public class IntelligentStockFeed extends AbstractStockFeed implements StockFeed {
	public static final Logger log = LoggerFactory.getLogger(IntelligentStockFeed.class.getName());

	public static boolean refresh = true;

	public static Optional<StockV1> getFlatCashSeries(final Instrument instrument, final int years) throws IOException {
		return IntelligentStockFeed.getFlatCashSeries(instrument, LocalDate.now().minusYears(years), LocalDate.now());
	}

	public static Optional<StockV1> getFlatCashSeries(final Instrument instrument, final LocalDate fromDate,
			final LocalDate toDate) throws IOException {
		final StockV1 cash = new StockV1(instrument);
		final List<Bar> history = Lists.newArrayList();
		history.add(new ExtendedHistoricalQuote(instrument.getCode(), toDate, BigDecimal.ONE, BigDecimal.ONE,
				BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, DoubleNum.valueOf(0l), "Manually created"));

		final FlatLineInterpolator flatLineInterpolator = new FlatLineInterpolator();
		cash.setHistory(flatLineInterpolator.extendToFromDate(history, fromDate));
		cash.setQuote(new StockQuoteBuilder(instrument).setPrice(DoubleNum.valueOf(1)).build());
		return Optional.of(cash);
	}

	public static void setRefresh(final boolean refresh) {
		IntelligentStockFeed.refresh = refresh;
	}

	public void addLatestQuoteToTheSeries(final StockV1 stock, final QuoteFeed dataFeed) throws IOException {
		// Add latest price to the series
		if ((dataFeed != null) && dataFeed.isAvailable()) {
			final ExtendedStockQuote quote = dataFeed.getStockQuote(stock.getInstrument());
			if ((quote != null) && quote.isPopulated()) {
				LocalDate calendarToLocalDate = DateUtils.calendarToLocalDate(quote.getLastTradeTime());
				List<Bar> history = stock.getHistory();
				if (!history.isEmpty()) {
					Bar mostRecentQuote = TimeseriesUtils.getMostRecentQuote(history);
					if (mostRecentQuote.getEndTime().toLocalDate().isEqual(calendarToLocalDate)) {
						history.remove(mostRecentQuote);
					}
					history.add(new ExtendedHistoricalQuote(stock.getInstrument().code(), calendarToLocalDate,
							quote.getOpen(), quote.getDayLow(), quote.getDayHigh(), quote.getPrice(), quote.getPrice(),
							DoubleNum.valueOf(quote.getVolume()), Source.Yahoo.name()));
				}
			}
		} else {
			IntelligentStockFeed.log.warn(String.format("Failed to populate quote for %s", stock.getInstrument()));
		}

	}

	@Override
	public Optional<StockV1> get(final Instrument instrument, final int years) {
		return this.get(instrument, LocalDate.now().minusYears(years), LocalDate.now(), false);
	}

	@Override
	public Optional<StockV1> get(final Instrument instrument, final int years, final boolean interpolate)
			throws IOException {
		return this.get(instrument, LocalDate.now().minusYears(years), LocalDate.now(), interpolate);
	}

	@Override
	public Optional<StockV1> get(final Instrument instrument, final LocalDate fromDate, final LocalDate toDate)
			throws IOException {
		return this.get(instrument, fromDate, toDate, false);
	}

	@Override
	public Optional<StockV1> get(final Instrument instrument, final LocalDate fromDateRaw, final LocalDate toDateRaw,
			final boolean interpolate) {
		try {

			// Ignore weekends
			LocalDate fromDate = DateUtils.getLastWeekday(fromDateRaw);
			LocalDate toDate = DateUtils.getLastWeekday(toDateRaw);
			
			if (instrument.equals(Instrument.CASH)) {
				return IntelligentStockFeed.getFlatCashSeries(instrument, fromDate, toDate);
			}
			
			final CachedStockFeed cachedDataFeed = (CachedStockFeed) StockFeedFactory.getDataFeed(Source.MANUAL);

			final Optional<StockV1> cachedData = this.getDataIfFeedAvailable(instrument, fromDate, toDate,
					cachedDataFeed, true);

			StockFeed webDataFeed = StockFeedFactory.getDataFeed(instrument.getSource());

			// If we have the data already, don't bother to refresh
			// Note will need to update today's live quote still though,
			// so skip latest date point
			Optional<StockV1> liveData = Optional.empty();
			boolean getWebData = IntelligentStockFeed.refresh
					&& (webDataFeed.isAvailable() || StockFeedFactory.getDataFeed(Source.Google).isAvailable());
			if (getWebData) {
				if (cachedData.isPresent()) {
					final List<Bar> cachedHistory = cachedData.get().getHistory();
					List<LocalDate> missingDates = TimeseriesUtils.getMissingDataPoints(cachedHistory, fromDate,
							DateUtils.getPreviousDate(toDate));

					if (!missingDates.isEmpty()) {
						liveData = this.getDataIfFeedAvailable(instrument, missingDates.get(0),
								missingDates.get(missingDates.size() - 1), webDataFeed, IntelligentStockFeed.refresh);
					}
				} else {
					liveData = this.getDataIfFeedAvailable(instrument, fromDate, toDate, webDataFeed,
							IntelligentStockFeed.refresh);
				}
			}

			if (liveData.isPresent()) {
				final StockV1 stock = liveData.get();
				if (cachedData.isPresent()) {
					this.mergeSeries(cachedData.get(), stock.getHistory(), cachedData.get().getHistory());
				}
				cachedDataFeed.storeSeries(stock);
				this.addLatestQuoteToTheSeries(liveData.get(), StockFeedFactory.getQuoteFeed(Source.Yahoo));
			} else if (cachedData.isPresent()) {
				liveData = cachedData;
			} else {
				IntelligentStockFeed.log.warn("No data for " + instrument);
				return Optional.empty();
			}

			TimeseriesUtils.cleanUpSeries(liveData);
			return TimeseriesUtils.interpolateAndSortSeries(fromDate, toDate, interpolate, liveData);
		} catch (final Exception e) {
			IntelligentStockFeed.log.warn(e.getMessage());
			return Optional.empty();
		}

	}

	public Optional<StockV1> get(final Instrument instrument, final String fromDate, final String toDate,
			final boolean interpolate) {
		return this.get(instrument, LocalDate.parse(fromDate), LocalDate.parse(toDate), interpolate);
	}

	public Optional<StockV1> getDataIfFeedAvailable(final Instrument instrument, final LocalDate fromDate,
			final LocalDate toDate, final StockFeed dataFeed, final boolean useFeed) throws IOException {
		final Optional<StockV1> data;
		if (useFeed) {
			if (dataFeed.isAvailable()) {
				data = dataFeed.get(instrument, fromDate, toDate);
			} else {
				IntelligentStockFeed.log.warn(dataFeed.getClass().getName() + " is not available");
				data = Optional.empty();
			}
		} else {
			data = Optional.empty();
		}
		return data;
	}

	@Override
	public Source getSource() {
		return Source.MANUAL;
	}

	@Override
	public boolean isAvailable() {
		return StockFeedFactory.getDataFeed(Source.MANUAL).isAvailable()
				|| StockFeedFactory.getDataFeed(Source.Google).isAvailable()
				|| StockFeedFactory.getDataFeed(Source.Yahoo).isAvailable();
	}

}
