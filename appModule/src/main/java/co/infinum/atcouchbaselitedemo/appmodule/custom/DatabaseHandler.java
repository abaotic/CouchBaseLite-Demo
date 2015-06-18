package co.infinum.atcouchbaselitedemo.appmodule.custom;

import com.couchbase.lite.BlobStore;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.android.AndroidContext;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by anabaotic on 4/1/14.
 */
public class DatabaseHandler {

    private static Manager manager;

    private static Database database;

    private static String DBNAME = "atalks_demo_db";

    public static final String SYNC_URL = "https://izelsoolsomeldomencessel:EsNRGjdo4bGxg51pA34j5VgT@abaotic.cloudant.com/atalks_demo_db";


    public static Manager getManager(Context context) {

        if (manager == null) {
            initManager(context);
        }
        return manager;
    }


    private static void initManager(Context context) {

        try {
            manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Database getDatabase(Context context) {

        if (database == null) {
            initDatabase(context);
        }

        return database;
    }

    private static void initDatabase(Context context) {

        try {
            database = getManager(context).getDatabase(DBNAME);
        } catch (CouchbaseLiteException ex) {

        }


    }

    public static HashMap<String, Object> getAllDocuments(Context context, String dbName) {

        HashMap<String, Object> documentMap = new HashMap<String, Object>();

        Query query = getDatabase(context).createAllDocumentsQuery();
        query.setDescending(true);
        try {
            QueryEnumerator rowEnum = query.run();
            for (Iterator<QueryRow> it = rowEnum; it.hasNext(); ) {
                QueryRow row = it.next();
                documentMap.put(row.getDocumentId(), row.getDocument());

            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return documentMap;
    }

    public static ArrayList<String> getItems(Context context, String DBNAME) {

        HashMap<String, Object> documents = getAllDocuments(context, DBNAME);
        ArrayList<String> items = getItems(documents);

        return items;
    }

    public static ArrayList<String> getItems(HashMap<String, Object> docs) {

        HashMap<String, Object> documents = docs;
        ArrayList<String> items = new ArrayList<String>();

        for (Map.Entry<String, Object> objectEntry : documents.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(objectEntry.getKey()).append("\n");
            for (Map.Entry<String, Object> entry : ((Document) objectEntry.getValue()).getProperties().entrySet()) {
                sb.append(String.valueOf(entry)).append("\n");
            }
            items.add(sb.toString());
            Log.d("ANA document", sb.toString());

        }

        return items;
    }

    public static ArrayList<Document> getDocuments(HashMap<String, Object> docs) {

        HashMap<String, Object> documents = docs;
        ArrayList<Document> items = new ArrayList<Document>();
        for (Map.Entry<String, Object> objectEntry : documents.entrySet()) {
            items.add((Document) objectEntry.getValue());
        }

        return items;
    }

    public static Document getDocumentById(Context context, String dbname, String id) {

        Document doc = getDatabase(context).getDocument(id);
        return doc;
    }

    public static BlobStore getAttachments() {

        return database.getAttachments();
    }

    protected void startCBLite(Context context) throws Exception {

        manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
        database = manager.getDatabase(DBNAME);

    }


}
