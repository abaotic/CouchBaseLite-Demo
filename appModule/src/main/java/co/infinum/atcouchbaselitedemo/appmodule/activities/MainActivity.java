package co.infinum.atcouchbaselitedemo.appmodule.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.couchbase.lite.Database;
import com.couchbase.lite.DocumentChange;
import com.couchbase.lite.replicator.Replication;

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

	private ProgressBar downStreamProgress;
	private ProgressBar upStreamProgress;

	private LinearLayout llUpstream;
	private LinearLayout llDownstream;
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

		downStreamProgress = (ProgressBar) findViewById(R.id.progressBarDownstream);
		upStreamProgress = (ProgressBar) findViewById(R.id.progressBarUpstream);
		llDownstream = (LinearLayout) findViewById(R.id.llDownstream);
		llUpstream = (LinearLayout) findViewById(R.id.llUpstream);
	}

	protected void initDB() {

		database = DatabaseHandler.getDatabase(this);
		database.addChangeListener(databaseListener);

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

				llDownstream.setVisibility(View.GONE);
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

				llUpstream.setVisibility(View.GONE);
			String msg = String.format("Replicator processed %d / %d", processed, total);
			Log.d("CBLITE", msg);
		}

	}

	protected void onDatabaseChangeEvent(Database.ChangeEvent event) {

		List<DocumentChange> documentChanges = event.getChanges();
		if (documentChanges != null && documentChanges.size() > 0) {
			Toast.makeText(MainActivity.this, documentChanges.size() + " documents changed!", Toast.LENGTH_SHORT).show();
		}
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

}
