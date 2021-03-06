package com.leonarduk.finance.portfolio;

import java.util.Set;

public class Portfolio {
	private Set<Position> holdings;

	public Portfolio(final Set<Position> positions) {
		this.holdings = positions;
	}

	public Set<Position> getHoldings() {
		return this.holdings;
	}

	public void setHoldings(final Set<Position> holdings) {
		this.holdings = holdings;
	}

}
