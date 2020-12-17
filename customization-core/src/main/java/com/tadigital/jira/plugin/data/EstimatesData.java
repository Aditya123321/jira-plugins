package com.tadigital.jira.plugin.data;

public class EstimatesData {
	
	private double architectEstimate;
	private double originalEstimate;
	private double actualEstimate;
	private double remainingEstimate;
	private long devEstimateDeviation;
	private long architectDevDeviation;
	private long architectActualDeviation;
	
	public double getArchitectEstimate() {
		return architectEstimate;
	}

	public void setArchitectEstimate(double architectEstimate) {
		this.architectEstimate = architectEstimate;
	}

	public double getOriginalEstimate() {
		return originalEstimate;
	}

	public void setOriginalEstimate(double originalEstimate) {
		this.originalEstimate = originalEstimate;
	}

	public double getActualEstimate() {
		return actualEstimate;
	}

	public void setActualEstimate(double actualEstimate) {
		this.actualEstimate = actualEstimate;
	}

	public double getRemainingEstimate() {
		return remainingEstimate;
	}

	public void setRemainingEstimate(double remainingEstimate) {
		this.remainingEstimate = remainingEstimate;
	}

	public long getDevEstimateDeviation() {
		return devEstimateDeviation;
	}

	public void setDevEstimateDeviation(long devEstimateDeviation) {
		this.devEstimateDeviation = devEstimateDeviation;
	}

	public long getArchitectDevDeviation() {
		return architectDevDeviation;
	}

	public void setArchitectDevDeviation(long architectDevDeviation) {
		this.architectDevDeviation = architectDevDeviation;
	}

	public long getArchitectActualDeviation() {
		return architectActualDeviation;
	}

	public void setArchitectActualDeviation(long architectActualDeviation) {
		this.architectActualDeviation = architectActualDeviation;
	}
}
