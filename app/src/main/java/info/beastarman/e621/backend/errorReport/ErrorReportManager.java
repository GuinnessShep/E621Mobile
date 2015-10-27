package info.beastarman.e621.backend.errorReport;

import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import info.beastarman.e621.backend.PersistentHttpClient;

/**
 * Created by beastarman on 10/17/2015.
 */
public class ErrorReportManager
{
	private static ErrorReportAPI api = new ErrorReportAPI(new PersistentHttpClient(new DefaultHttpClient(),3), "http://beastarman.info/report/");
	private String app_id;
	private String user = "user";
	ErrorReportStorageInterface errorReportStorage;
	ErrorReportPendingStorageInterface pendingStorageInterface;

	public ErrorReportManager(String app_id, File basePath)
	{
		this.app_id = app_id;
		basePath.mkdirs();
		this.pendingStorageInterface = new ErrorReportPendingStorage(new File(basePath,"pendingReportStorage/"));
		this.errorReportStorage = new ErrorReportStorage(new File(basePath,"reportStorage/"));
	}

	public void sendReport(ErrorReportReport report)
	{
		try
		{
			ErrorReportReportResponse response = api.report(app_id, report.text, report.log, report.tags);

			if(response.isSuccessfull())
			{
				report.hash = response.getHash();

				errorReportStorage.addReport(report);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();

			pendingStorageInterface.addReport(report);
		}
	}

	public void sendPendingReports()
	{
		ArrayList<ErrorReportReport> reports = pendingStorageInterface.getReports();

		for(ErrorReportReport report : reports)
		{
			try
			{
				ErrorReportReportResponse response = api.report(app_id, report.text, report.log, report.tags);

				pendingStorageInterface.removeReport(report.hash);

				if(response.isSuccessfull())
				{
					report.hash = response.getHash();

					errorReportStorage.addReport(report);
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public ArrayList<ErrorReportMessage> updateUnreadMessages()
	{
		ArrayList<ErrorReportReport> reports = errorReportStorage.getReports();
		ArrayList<ErrorReportMessage> newMessages = new ArrayList<ErrorReportMessage>();

		for(ErrorReportReport report : reports)
		{
			try
			{
				ErrorReportGetMessagesResponse response = api.getMessages(report.hash);

				int lastId = errorReportStorage.getLastMessageID(report.hash);

				newMessages.addAll(response.messages.subList(lastId,response.maxId));

				errorReportStorage.updateLastMessageID(report.hash, response.maxId);
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}

		return newMessages;
	}
}
