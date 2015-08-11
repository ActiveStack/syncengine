package com.percero.defaults;

import org.springframework.stereotype.Component;
import com.googlecode.flyway.core.Flyway;

@Component
public class EmptyFlyway {

	private Flyway percInit;
	private Flyway percMigrate;
	private Flyway percClean;
	/**
	 * @return the percInit
	 */
	public Flyway getPercInit() {
		return percInit;
	}
	/**
	 * @param percInit the percInit to set
	 */
	public void setPercInit(Flyway percInit) {
		this.percInit = percInit;
	}
	/**
	 * @return the percMigrate
	 */
	public Flyway getPercMigrate() {
		return percMigrate;
	}
	/**
	 * @param percMigrate the percMigrate to set
	 */
	public void setPercMigrate(Flyway percMigrate) {
		this.percMigrate = percMigrate;
	}
	/**
	 * @return the percClean
	 */
	public Flyway getPercClean() {
		return percClean;
	}
	/**
	 * @param percClean the percClean to set
	 */
	public void setPercClean(Flyway percClean) {
		this.percClean = percClean;
	}
}
