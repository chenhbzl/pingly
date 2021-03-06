/*
 *    Pingly - A simple app for checking for signs of life in hosts/services.
 *    Copyright 2012 Barry O'Neill
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package net.nologin.meep.pingly.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import net.nologin.meep.pingly.R;
import net.nologin.meep.pingly.adapter.ProbeListCursorAdapter;
import net.nologin.meep.pingly.alarm.AlarmScheduler;
import net.nologin.meep.pingly.model.ScheduleEntry;
import net.nologin.meep.pingly.model.probe.Probe;
import net.nologin.meep.pingly.util.PinglyUtils;

import java.util.List;

import static net.nologin.meep.pingly.PinglyConstants.LOG_TAG;

/**
 * Lists the probles currently configured probes
 */
public class ProbeListActivity extends BasePinglyActivity {

	private ProbeListCursorAdapter listAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.probe_list);

		Cursor allProbesCursor = probeDAO.findAllProbes();
		listAdapter = new ProbeListCursorAdapter(this, allProbesCursor);
		ListView lv = (ListView) findViewById(R.id.probeList);
		lv.setAdapter(listAdapter);
		registerForContextMenu(lv);

		View empty = findViewById(R.id.emptyListElem);
		lv.setEmptyView(empty);

		startManagingCursor(allProbesCursor);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int itemPos, long itemId) {
				Log.d(LOG_TAG, "Got id " + itemId);
				// onclick used to just start the runner, but I noticed that people then don't
				// realise that there's a long-press menu with other features
				// PinglyUtils.startActivityProbeRunner(ProbeListActivity.this,itemId);

				// onclick == long press, ie, open the context menu
				openContextMenu(view);
			}
		});

	}


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
									ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);

		if (v.getId() == R.id.probeList) {

			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

			Probe target = probeDAO.findProbeById(info.id);
			menu.setHeaderTitle(target.name);

			MenuInflater inflater1 = getMenuInflater();
			inflater1.inflate(R.menu.probe_list_context, menu);

		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();

		final Probe probe = probeDAO.findProbeById(info.id);

		switch (item.getItemId()) {

			case R.id.probe_list_contextmenu_run:
				Log.d("PINGLY", "Running probe: " + probe);

				PinglyUtils.startActivityProbeRunner(this,probe);

				return true;

			case R.id.probe_list_contextmenu_edit:
				Log.d("PINGLY", "Editing probe: " + probe);

				PinglyUtils.startActivityProbeDetail(this,probe);

				return true;

			case R.id.probe_list_contextmenu_run_history:
				Log.d("PINGLY", "Going to run history for probe: " + probe);

				PinglyUtils.startActivityProbeRunHistory(this,probe);

				return true;

			case R.id.probe_list_contextmenu_schedule:

				Log.d("PINGLY", "Scheduling probe: " + probe);

				PinglyUtils.startActivityScheduleEntryDetail(this,probe);

				return true;

			case R.id.probe_list_contextmenu_delete:
				Log.d("PINGLY", "Deleting probe: " + probe);

				final List<ScheduleEntry> entries = scheduleDAO.findEntriesForProbe(probe.id);
				final int msgFmtRes = entries.size() > 0
						? R.string.dialog_probe_delete_plussched_confirmFmt
						: R.string.dialog_probe_delete_confirmFmt;

				String msg = getString(msgFmtRes, probe.name);

				AlertDialog dialog = new AlertDialog.Builder(this)
						.setTitle(R.string.dialog_probe_delete_title)
						.setMessage(msg)
						.setCancelable(false)
						.setPositiveButton(R.string.button_delete, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {

								// requestCancel alarms for those entries
								AlarmScheduler.cancelAlarms(ProbeListActivity.this, entries);

								// delete the run history (all items, not just finished)
								probeRunDAO.deleteHistoryForProbe(probe.id,false);

								// delete the entries
								scheduleDAO.deleteForProbe(probe.id);

								// then delete the probe
								probeDAO.deleteProbe(probe);

								/* since we stay where we are (no activity state change), the startManagingCursor()
								 * registration in onCreate() won't know to refresh the cursor/adapter.  We requery
								 * all probes and pass the new cursor to the adapter. */
								listAdapter.changeCursor(probeDAO.findAllProbes());

								// and finally, give the user some visual feedback
								PinglyUtils.showToast(ProbeListActivity.this, R.string.toast_probe_deleted, probe.name);

							}
						})
						.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						}).create();
				dialog.show();
				return true;

			default:
				Log.d("PINGLY", "Unhandled Item ID " + item.getItemId());
				super.onContextItemSelected(item);

		}

		// TextView text = (TextView)findViewById(R.id.footer);
		// text.setText(String.format("Selected %s for item %s", menuItemName,
		// listItemName));
		return true;
	}

}
