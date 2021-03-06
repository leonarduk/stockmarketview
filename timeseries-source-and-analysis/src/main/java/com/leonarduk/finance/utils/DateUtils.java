/*
 * Copyright (c) 2011 Kevin Sawicki <kevinsawicki@gmail.com>
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
package com.leonarduk.finance.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.jfree.data.time.Day;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.leonarduk.finance.stockfeed.feed.yahoofinance.YahooFeed;

/**
 * Helpers for common dates
 */
public class DateUtils {
	private static Map<String, Date> dates;

	public static final Logger logger = LoggerFactory.getLogger(DateUtils.class.getName());

	public static LocalDate calendarToLocalDate(Calendar calendar) {
		return LocalDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId()).toLocalDate();
	}

	public static Calendar dateToCalendar(final Date date) {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar;
	}

	public static Calendar dateToCalendar(final ZonedDateTime fromDate) {
		return DateUtils.dateToCalendar(convertToDateViaInstant(fromDate.toLocalDate()));
	}

	public static int getDiffInWorkDays(final LocalDate startDate, final LocalDate endDate) {
		return (int) daysBetween(startDate, endDate, ImmutableList.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
	}

	static long daysBetween(LocalDate start, LocalDate end, List<DayOfWeek> ignore) {
		return  Period.between(start, end).getDays();
	}

	private static String getDividendDateFormat(final String date) {
		if (date.matches("[0-9][0-9]-...-[0-9][0-9]")) {
			return "dd-MMM-yy";
		} else if (date.matches("[0-9]-...-[0-9][0-9]")) {
			return "d-MMM-yy";
		} else if (date.matches("...[ ]+[0-9]+")) {
			return "MMM d";
		} else {
			return "M/d/yy";
		}
	}

	public static Iterator<LocalDate> getLocalDateIterator(final LocalDate oldestDate, final LocalDate mostRecentDate) {
		return new Iterator<LocalDate>() {

			LocalDate nextDate = oldestDate;

			@Override
			public boolean hasNext() {
				return this.nextDate.isBefore(mostRecentDate) || this.nextDate.equals(mostRecentDate);
			}

			@Override
			public LocalDate next() {
				final LocalDate currentDate = this.nextDate;
				if (this.nextDate.getDayOfWeek() == DayOfWeek.FRIDAY) {
					this.nextDate = this.nextDate.plusDays(2);
				}
				this.nextDate = this.nextDate.plusDays(1);
				return currentDate;
			}

		};

	}

	public static Iterator<ZonedDateTime> getLocalDateNewToOldIterator(final ZonedDateTime startDate,
			final ZonedDateTime lastDate) {
		return new Iterator<ZonedDateTime>() {

			ZonedDateTime nextDate = startDate;

			@Override
			public boolean hasNext() {
				return this.nextDate.isAfter(lastDate) || this.nextDate.equals(lastDate);
			}

			@Override
			public ZonedDateTime next() {
				if (this.nextDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
					this.nextDate = this.nextDate.minusDays(2);
				}
				if (this.nextDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
					this.nextDate = this.nextDate.minusDays(1);
				}
				final ZonedDateTime currentDate = this.nextDate;
				this.nextDate = this.nextDate.minusDays(1);
				return currentDate;
			}

		};

	}

	public static LocalDate getPreviousDate(final LocalDate localDate) {
		final LocalDate returnDate = localDate.minusDays(1);
		return getLastWeekday(returnDate);
	}

	public static LocalDate getLastWeekday(final LocalDate returnDate) {
		if ((returnDate.getDayOfWeek() == DayOfWeek.SATURDAY) || (returnDate.getDayOfWeek() == DayOfWeek.SUNDAY)) {
			return DateUtils.getPreviousDate(returnDate);
		}
		return returnDate;
	}

	public static Date parseDate(final String fieldValue) throws ParseException {
		if (null == DateUtils.dates) {
			DateUtils.dates = Maps.newConcurrentMap();
		}
		return (DateUtils.dates.computeIfAbsent(fieldValue,
				v -> DateUtils.convertToDateViaInstant(LocalDate.parse(v, DateTimeFormatter.ISO_DATE))));
	}

	/**
	 * Used to parse the last trade date / time. Returns null if the date / time
	 * cannot be parsed.
	 *
	 * @param date     String received that represents the date
	 * @param time     String received that represents the time
	 * @param timeZone time zone to use for parsing the date time
	 * @return Calendar object with the parsed datetime
	 */
	public static Calendar parseDateTime(final String date, final String time, final TimeZone timeZone) {
		final String datetime = date + " " + time;
		final SimpleDateFormat format = new SimpleDateFormat("M/d/yyyy h:mma", Locale.US);

		format.setTimeZone(timeZone);
		try {
			if (StringUtils.isParseable(date) && StringUtils.isParseable(time)) {
				final Calendar c = Calendar.getInstance();
				c.setTime(format.parse(datetime));
				return c;
			}
		} catch (final ParseException ex) {
			DateUtils.logger.warn("Failed to parse datetime: " + datetime);
			DateUtils.logger.trace("Failed to parse datetime: " + datetime, ex);
		}
		return null;
	}

	/**
	 * Used to parse the dividend dates. Returns null if the date cannot be parsed.
	 *
	 * @param date String received that represents the date
	 * @return Calendar object representing the parsed date
	 */
	public static Calendar parseDividendDate(final String date) {
		if (!StringUtils.isParseable(date)) {
			return null;
		}
		final SimpleDateFormat format = new SimpleDateFormat(DateUtils.getDividendDateFormat(date.trim()), Locale.US);
		format.setTimeZone(TimeZone.getTimeZone(YahooFeed.TIMEZONE));
		try {
			final Calendar today = Calendar.getInstance(TimeZone.getTimeZone(YahooFeed.TIMEZONE));
			final Calendar parsedDate = Calendar.getInstance(TimeZone.getTimeZone(YahooFeed.TIMEZONE));
			parsedDate.setTime(format.parse(date.trim()));

			if (parsedDate.get(Calendar.YEAR) == 1970) {
				// Not really clear which year the dividend date is... making a
				// reasonable guess.
				final int monthDiff = parsedDate.get(Calendar.MONTH) - today.get(Calendar.MONTH);
				int year = today.get(Calendar.YEAR);
				if (monthDiff > 6) {
					year -= 1;
				} else if (monthDiff < -6) {
					year += 1;
				}
				parsedDate.set(Calendar.YEAR, year);
			}

			return parsedDate;
		} catch (final ParseException ex) {
			DateUtils.logger.warn("Failed to parse dividend date: " + date);
			DateUtils.logger.trace("Failed to parse dividend date: " + date, ex);
			return null;
		}
	}

	public static Date convertToDateViaInstant(LocalDate fromDate) {
		return java.util.Date.from(fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
	}

	public LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert) {
		return dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

//	public ZonedDateTime convertToLocalDateViaMilisecond(Date dateToConvert) {
//		return Instant.ofEpochMilli(dateToConvert.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
//	}

	public static ZonedDateTime calendarToZonedDateTime(Calendar lastTradeTime) {
		return lastTradeTime.toInstant().atZone(ZoneId.systemDefault());
	}

}
