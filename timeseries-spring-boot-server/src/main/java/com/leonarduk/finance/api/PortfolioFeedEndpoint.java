package com.leonarduk.finance.api;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.leonarduk.finance.stockfeed.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.leonarduk.finance.portfolio.Portfolio;
import com.leonarduk.finance.portfolio.Valuation;
import com.leonarduk.finance.portfolio.ValuationReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Named
@Path("/portfolio")
@SpringBootApplication
public class PortfolioFeedEndpoint {
    @Autowired
	private SnapshotAnalyser snapshotAnalyzer;

	@Autowired
	private DataStore dataStore;

	private final static Logger logger = LoggerFactory.getLogger(PortfolioFeedEndpoint.class.getName());

	public PortfolioFeedEndpoint() {
	}

	public PortfolioFeedEndpoint(final SnapshotAnalyser analyseSnapshot) {
		this.snapshotAnalyzer = analyseSnapshot;
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	@Path("extended")
	public String getExtendedAnalysis(@QueryParam("fromDate") final String fromDate,
			@QueryParam("toDate") final String toDate, @QueryParam("interpolate") final boolean interpolate,
			@QueryParam("clean") final boolean clean) throws IOException, URISyntaxException {
		return this.snapshotAnalyzer.createPortfolioReport(fromDate, toDate, interpolate, true, true, clean).toString();
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	@Path("analysis")
	public String getHistory(@QueryParam("fromDate") final String fromDate, @QueryParam("toDate") final String toDate,
			@QueryParam("interpolate") final boolean interpolate, @QueryParam("clean") final boolean clean)
			throws IOException, URISyntaxException {
		logger.info("getHistory:" + fromDate + " - " + toString());
		return this.snapshotAnalyzer.createPortfolioReport(fromDate, toDate, interpolate, false, true, clean)
				.toString();
	}

	// http://localhost:8091/portfolio/api/listnames/
	@Path("/api/listnames/")
	public Set<String> getPortfolios() throws IOException {
		PortfolioFeedEndpoint.logger.info("JSON query of positions");
		return this.snapshotAnalyzer.getPortfolios();
	}

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/api/display/")
	public Portfolio getPositions() throws IOException {
		PortfolioFeedEndpoint.logger.info("JSON query of positions");
		return new Portfolio(Sets.newHashSet(this.snapshotAnalyzer.getPositions()));
	}

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/api/report/")
	public List<ValuationReport> getValuations(@QueryParam("interpolate") final boolean interpolate,
			@QueryParam("clean") final boolean clean) throws IOException {
		PortfolioFeedEndpoint.logger.info("JSON query of valuations");

		final LocalDate fromDate = LocalDate.now().minusYears(2);
		final LocalDate toDate = LocalDate.now();

		final List<Valuation> valuations = this.snapshotAnalyzer.analayzeAllEtfs(this.snapshotAnalyzer.getPositions(),
				fromDate, toDate, interpolate, clean);

		return this.snapshotAnalyzer.getPortfolios().stream().map(portfolioName -> this.snapshotAnalyzer
				.createValuationReport(fromDate, toDate, valuations, portfolioName)).collect(Collectors.toList());

	}

}
