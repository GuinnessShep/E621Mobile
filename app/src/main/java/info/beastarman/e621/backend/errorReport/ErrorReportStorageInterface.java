package info.beastarman.e621.backend.errorReport;

import java.util.ArrayList;

/**
 * Created by beastarman on 10/19/2015.
 */
public interface ErrorReportStorageInterface
{
	void addReport(String report);

	ArrayList<String> getReports();
}