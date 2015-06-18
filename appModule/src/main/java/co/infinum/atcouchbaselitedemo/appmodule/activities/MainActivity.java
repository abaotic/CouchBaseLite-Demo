package co.infinum.atcouchbaselitedemo.appmodule.activities;

import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.DocumentChange;
import com.couchbase.lite.replicator.Replication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import co.infinum.atcouchbaselitedemo.appmodule.R;
import co.infinum.atcouchbaselitedemo.appmodule.custom.DatabaseHandler;


/**
 * Created by anabaotic on 4/1/14.
 */
public class MainActivity extends Activity {

    public static final String KEY_DATA = "data";

    public static final String KEY_ACTION = "action";

    public static final String UI_KEY_ACTION = "action";

    public static final String UI_KEY_PARAMS = "params";

    public static final String UI_KEY_TYPE = "type";

    public static final String UI_KEY_LABEL = "label";

    private ProgressBar downStreamProgress;

    private ProgressBar upStreamProgress;

    private LinearLayout llUpstream;

    private LinearLayout llDownstream;

    private LinearLayout llDataHolder;

    private LayoutInflater inflater;

    private Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        initDB();
        startSync();

    }

    private void initUI() {

        inflater = getLayoutInflater();
        downStreamProgress = (ProgressBar) findViewById(R.id.progressBarDownstream);
        upStreamProgress = (ProgressBar) findViewById(R.id.progressBarUpstream);
        llDownstream = (LinearLayout) findViewById(R.id.llDownstream);
        llUpstream = (LinearLayout) findViewById(R.id.llUpstream);
        llDataHolder = (LinearLayout) findViewById(R.id.llDataHodler);
    }

    protected void initDB() {

        database = DatabaseHandler.getDatabase(this);
        database.addChangeListener(databaseListener);
        Document document = database.getDocument("app_screen_profile");
        displayData(document, false);

    }

    protected void startSync() {

        URL syncUrl;
        try {
            syncUrl = new URL(DatabaseHandler.SYNC_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        Replication pullReplication = database.createPullReplication(syncUrl);
        pullReplication.setContinuous(true);

        Replication pushReplication = database.createPushReplication(syncUrl);
        pushReplication.setContinuous(true);

        pullReplication.addChangeListener(pullReplicationListener);
        pushReplication.addChangeListener(pushReplicationListener);

        pullReplication.start();
        pushReplication.start();

    }

    private void displayData(Document doc, boolean clearFirst) {

        if (clearFirst) {
            llDataHolder.removeAllViews();
        }
        setAvatar(doc);
        try {
            Map<String, String> dataObject = (Map<String, String>) doc.getProperties().get(KEY_DATA);
            for (Map.Entry<String, String> dataEntry : dataObject.entrySet()) {
                View dataItem = inflater.inflate(R.layout.list_item_profile_detail, null);
                ((TextView) dataItem.findViewById(R.id.tvParamLabel)).setText(String.valueOf(dataEntry.getKey()));
                ((TextView) dataItem.findViewById(R.id.tvParamContent)).setText(String.valueOf(dataEntry.getValue()));
                llDataHolder.addView(dataItem, llDataHolder.getChildCount());

            }
            final Map<String, String> actionObject = (Map<String, String>) doc.getProperties().get(KEY_ACTION);
            View actionView = null;
            if (String.valueOf(actionObject.get(UI_KEY_TYPE)).equals("button")) {
                actionView = inflater.inflate(R.layout.list_item_profile_action_button, null);
                Button action = (Button) actionView.findViewById(R.id.viewAction);
                action.setText(String.valueOf(actionObject.get(UI_KEY_LABEL)));
                action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Intent i = new Intent(String.valueOf(actionObject.get(UI_KEY_ACTION)),
                                Uri.parse(String.valueOf(actionObject.get(UI_KEY_PARAMS))));
                        startActivity(i);
                    }
                });
            } else {
                //cover other types
            }
            llDataHolder.addView(actionView, llDataHolder.getChildCount());
        } catch (Exception e) {
            System.out.println(e.toString());

            return;
        }


    }

    protected Database.ChangeListener databaseListener = new Database.ChangeListener() {
        @Override
        public void changed(final Database.ChangeEvent event) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    onDatabaseChangeEvent(event);
                }
            });
        }
    };

    protected Replication.ChangeListener pushReplicationListener = new Replication.ChangeListener() {
        @Override
        public void changed(final Replication.ChangeEvent event) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    onPushReplicationEvent(event);
                }
            });


        }
    };

    protected Replication.ChangeListener pullReplicationListener = new Replication.ChangeListener() {
        @Override
        public void changed(final Replication.ChangeEvent event) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    onPullReplicationEvent(event);
                }
            });


        }
    };


    protected void onPullReplicationEvent(Replication.ChangeEvent event) {

        llDownstream.setVisibility(View.VISIBLE);
        Replication replication = event.getSource();

        if (!replication.isRunning()) {
            Log.d("CBLITE", String.format("Replicator %s not running", replication));
        } else {
            final int processed = replication.getCompletedChangesCount();
            final int total = replication.getChangesCount();

            downStreamProgress.setProgress((int) ((double) processed * 100 / total));
            if (processed == total)

            {
                llDownstream.setVisibility(View.GONE);
            }
            Map<String, Object> headers = replication.getHeaders();
            List<String> ids = replication.getDocIds();
            String msg = String.format("Replicator processed %d / %d", processed, total);
            Log.d("CBLITE", msg);
        }
    }

    protected void onPushReplicationEvent(Replication.ChangeEvent event) {

        llUpstream.setVisibility(View.VISIBLE);
        Replication replication = event.getSource();
        if (!replication.isRunning()) {
            Log.d("CBLITE", String.format("Replicator %s not running", replication));
        } else {
            final int processed = replication.getCompletedChangesCount();
            final int total = replication.getChangesCount();
            Map<String, Object> headers = replication.getHeaders();

            upStreamProgress.setProgress((int) ((double) processed * 100 / total));
            if (processed == total)

            {
                llUpstream.setVisibility(View.GONE);
            }
            String msg = String.format("Replicator processed %d / %d", processed, total);
            Log.d("CBLITE", msg);
        }

    }

    protected void onDatabaseChangeEvent(Database.ChangeEvent event) {

        List<DocumentChange> documentChanges = event.getChanges();
        if (documentChanges != null && documentChanges.size() > 0) {
//			Toast.makeText(MainActivity.this, documentChanges.size() + " documents changed!", Toast.LENGTH_SHORT).show();
        }
        displayData(database.getDocument("app_screen_profile"), true);
    }

    protected void onDestroy() {

        try {
            if (DatabaseHandler.getManager(this) != null) {
                DatabaseHandler.getManager(this).close();
            }
        } catch (NetworkOnMainThreadException nomtException) {

        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setAvatar(Document doc) {

        if (doc == null || doc.getCurrentRevision() == null || doc.getCurrentRevision().getAttachments() == null) {
            return;
        }
        for (Attachment at : doc.getCurrentRevision().getAttachments()) {
            if (at.getContentType().toLowerCase().contains("image/")) {
                View images = inflater.inflate(R.layout.list_item_profile_avatar, null);
                ImageView iv = (ImageView) images.findViewById(R.id.ivAvatar);

                try {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    byte[] buffer = new byte[0xFFFF];

                    for (int len; (len = at.getContent().read(buffer)) != -1; ) {
                        os.write(buffer, 0, len);
                    }

                    os.flush();

                    byte[] bytes = os.toByteArray();
                    iv.setImageBitmap(decodeBase64(bytes));
                    llDataHolder.addView(images, llDataHolder.getChildCount());
                    os.close();

                } catch (CouchbaseLiteException ex) {
                } catch (IOException ex2) {
                }
            }

        }
    }

    public static Bitmap decodeBase64(String input) {

        byte[] decodedByte = Base64.decode(input, 0);
        return decodeBase64(decodedByte);
    }

    public static Bitmap decodeBase64(byte[] decodedByte) {

        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }
}
