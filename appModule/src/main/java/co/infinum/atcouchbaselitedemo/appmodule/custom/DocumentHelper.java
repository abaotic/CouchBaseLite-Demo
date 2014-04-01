package co.infinum.atcouchbaselitedemo.appmodule.custom;

import android.util.Log;

import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anabaotic on 3/18/14.
 */
public class DocumentHelper {

	public static final String ACTION_KEY = "action";
	public static final String ACTION_NAME_KEY = "action_name";
	public static final String PARAMETERS_KEY = "parameters";

	public static boolean documentHasAction(Document document) {

		Map<String, Object> options = document.getProperties();

		return document != null && document.getProperties() != null && document.getProperties().containsKey(ACTION_KEY);
	}

	public static String getActionFromDocument(Document document) {

		String actionString = "";
		if (document != null && document.getProperties() != null && document.getProperties().containsKey(ACTION_KEY)) {
			actionString = String.valueOf(document.getProperty(ACTION_KEY));
		}
		return actionString;
	}

	public static String[] getParameters(Document document) {

		String prms = String.valueOf(document.getProperty(PARAMETERS_KEY));
		String[] params = new String[]{};
		try {
			JSONArray array = new JSONArray(prms);
			params = new String[array.length()];
			for (int i = 0; i < array.length(); i++) {
				Log.d("ANA param ", array.getString(i));
				params[i] = array.getString(i);
			}
		} catch (JSONException ex) {
			System.out.println(ex);
		}
		return params;
	}

	public static String getActionName(Document document) {

		return String.valueOf(document.getProperties().get(ACTION_NAME_KEY));
	}

	public static HashMap<String, String> getJSModulesFromAttachments(Document document) {

		HashMap<String, String> jsModules = new HashMap<String, String>();

		if (documentHasAttachment(document)) {
			List<Attachment> attachments = document.getCurrentRevision().getAttachments();
			for (Attachment attachment : attachments) {
				if (attachment.getContentType().equals("text/javascript")) {
					Log.d("ANA name ", attachment.getName());
					Log.d("ANA cntType", attachment.getContentType());
					Log.d("ANA doc id", attachment.getDocument().getId());
					Log.d("ANA isGzipped", attachment.getGZipped() + "");
					Log.d("ANA length", attachment.getLength() + "");
					Log.d("ANA metadata", String.valueOf(attachment.getMetadata()));
					String content = attachmentToString(attachment);
					Log.d("ANA name ", content);
					jsModules.put(attachment.getName(), content);

				}
			}
		}

		return jsModules;
	}


	public static boolean documentHasAttachment(Document document) {

		return document != null && document.getCurrentRevision().getAttachments() != null && document.getCurrentRevision().getAttachments().size() > 0;
	}

	private static String attachmentToString(Attachment at) {

		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			byte[] buffer = new byte[0xFFFF];

			for (int len; (len = at.getContent().read(buffer)) != -1; )
				os.write(buffer, 0, len);

			os.flush();
			String content = os.toString();
			os.close();
			return content;


		} catch (CouchbaseLiteException ex) {
		} catch (IOException ex2) {
		}
		return "";
	}




}
