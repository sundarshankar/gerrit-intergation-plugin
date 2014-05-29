package com.pawelmaslyk.gerritintegration4sonar;

import com.pawelmaslyk.gerritintegration4sonar.sonar.SonarAnalysisResult;
import com.pawelmaslyk.gerritintegration4sonar.sonar.SonarAnalysisStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.measures.Metric;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * I contain logic to assess sonar analysis success
 * 
 * @author pawel
 * 
 */
public class SonarResultEvaluator implements BatchExtension {

	private static final Logger logger = LoggerFactory.getLogger(SonarResultEvaluator.class);


	/**
	 * I determine how successful was sonar analysis
	 * 
	 * @param context
	 *            the sensor context
	 * @param dashboardUrl
	 *            The Sonar dashboard URL to include in the result message
	 * @return the sonar analysis result
	 */
	public SonarAnalysisResult getResult(SensorContext context, String dashboardUrl) {
		SonarAnalysisStatus status = SonarAnalysisStatus.NO_PROBLEMS;

		StringBuilder messageBuilder = new StringBuilder("Sonar analysis (").append(dashboardUrl).append("):\n");

		List<Measure> errors = getErrors(context);
		List<Measure> warnings = getWarnings(context);
		if (warnings.size() > 0) {
			status = SonarAnalysisStatus.WARNINGS;
			messageBuilder.append("  Warnings:");
			for (Measure warning : warnings) {
				messageBuilder.append("\n    ");
				messageBuilder.append(warning.getAlertText());
			}
		}
		if (errors.size() > 0) {
			if (status == SonarAnalysisStatus.WARNINGS) {
				messageBuilder.append("\n");
			}

			status = SonarAnalysisStatus.ERRORS;
			messageBuilder.append("  Errors:");
			for (Measure error : errors) {
				messageBuilder.append("\n    ");
				messageBuilder.append(error.getAlertText());
			}
		}
		if (status == SonarAnalysisStatus.NO_PROBLEMS) {
			messageBuilder.append("  No alerts.");
		}

		return new SonarAnalysisResult(messageBuilder.toString(), status);
	}

	private List<Measure> getErrors(SensorContext context) {
		List<Measure> errors = new ArrayList<Measure>();

		Collection<Measure> measures = context.getMeasures(MeasuresFilters.all());
		for (Measure measure : measures) {
			if (isErrorAlert(measure)) {
				logger.error(measure.getAlertText());
				errors.add(measure);
			}
		}

		return errors;
	}

	private List<Measure> getWarnings(SensorContext context) {
		List<Measure> warnings = new ArrayList<Measure>();

		Collection<Measure> measures = context.getMeasures(MeasuresFilters.all());
		for (Measure measure : measures) {
			if (isWarningAlert(measure)) {
				logger.warn(measure.getAlertText());
				warnings.add(measure);
			}
		}
		return warnings;
	}

	private boolean isWarningAlert(Measure measure) {
		return !measure.getMetric().equals(CoreMetrics.ALERT_STATUS)
		                && Metric.Level.WARN.equals(measure.getAlertStatus());
	}

	private boolean isErrorAlert(Measure measure) {
		return !measure.getMetric().equals(CoreMetrics.ALERT_STATUS)
		                && Metric.Level.ERROR.equals(measure.getAlertStatus());
	}
}
