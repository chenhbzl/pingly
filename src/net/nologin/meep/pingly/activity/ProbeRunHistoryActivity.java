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
import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import net.nologin.meep.pingly.R;
import net.nologin.meep.pingly.adapter.ProbeRunHistoryCursorAdapter;
import net.nologin.meep.pingly.model.ProbeRun;
import net.nologin.meep.pingly.model.ProbeRunStatus;
import net.nologin.meep.pingly.model.probe.Probe;
import net.nologin.meep.pingly.util.PinglyUtils;
import net.nologin.meep.pingly.util.StringUtils;
import net.nologin.meep.pingly.view.PinglyProbeDetailsView;


import static net.nologin.meep.pingly.PinglyConstants.LOG_TAG;

/**
 * Show the log of previous runs of the selected probe
 */
public class ProbeRunHistoryActivity extends BasePinglyActivity {

	static final int DIALOG_PROBE_RUN_LOG = 1;

	ProbeRunHistoryCursorAdapter listAdapter;
	Probe currentProbe;
	ProbeRun probeRunForLogDialog; // for showing chosen log info

	@Override
	protected void onCreate(Bundle state) {

		super.onCreate(state);
		setContentView(R.layout.probe_run_history_list);

		currentProbe = getIntentExtraProbe();

		// if a probe run param is supplied, that overrides the probe param
		probeRunForLogDialog = getIntentExtraProbeRun();
		if(probeRunForLogDialog != null){
			currentProbe = probeRunForLogDialog.probe;
		}

		Long probeId = currentProbe == null ? null : currentProbe.id;

		Cursor runHistoryCursor = probeRunDAO.queryForProbeRunHistoryCursorAdapter(probeId);
		listAdapter = new ProbeRunHistoryCursorAdapter(this, runHistoryCursor);
		ListView lv = (ListView) findViewById(R.id.probeRunList);
		lv.setAdapter(listAdapter);
		registerForContextMenu(lv);

		View empty = findViewById(R.id.emptyListElem);
		lv.setEmptyView(empty);

		startManagingCursor(runHistoryCursor);

		PinglyProbeDetailsView probeDetails = (PinglyProbeDetailsView)findViewById(R.id.probeSummaryHeader);
		probeDetails.initForProbe(currentProbe,true);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int itemPos, long itemId) {
				Log.d(LOG_TAG, "Got id " + itemId);

				probeRunForLogDialog = probeRunDAO.findProbeRunById(itemId);
				showDialog(DIALOG_PROBE_RUN_LOG);

			}
		});

		findViewById(R.id.probeHistory_runNowBut).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				PinglyUtils.startActivityProbeRunner(ProbeRunHistoryActivity.this,currentProbe);
			}
		});

		findViewById(R.id.but_clearHistory).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				// delete any finished probes
				probeRunDAO.deleteHistoryForProbe(currentProbe.id, true);
				listAdapter.changeCursor(probeRunDAO.queryForProbeRunHistoryCursorAdapter(currentProbe.id));
			}
		});

	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		// so we know what probe run log to display after sleep/rotation etc
		if(probeRunForLogDialog != null){
			savedInstanceState.putLong(STATE_PROBERUN_ID, probeRunForLogDialog.id);
		}
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		// we were previously running a probe, get the id of that run
		if (savedInstanceState.containsKey(STATE_PROBERUN_ID)) {
			long runId = savedInstanceState.getLong(STATE_PROBERUN_ID);
			Log.d(LOG_TAG, "onRestoreInstanceState, we were processing probe run: " + runId);
			probeRunForLogDialog = probeRunDAO.findProbeRunById(runId);
		}
	}


	protected Dialog onCreateDialog(int id) {

		Dialog dialog;
		AlertDialog.Builder builder;

		switch (id) {
			case DIALOG_PROBE_RUN_LOG:

				View logView = View.inflate(PinglyUtils.getPinglyDialogContext(ProbeRunHistoryActivity.this),
						R.layout.probe_run_history_log, null);

				builder = PinglyUtils.getAlertDialogBuilder(this);
				builder.setView(logView)
						.setCancelable(true)
						.setNegativeButton(R.string.button_close, null);
				dialog = builder.create();

				break;

			default:
				Log.e(LOG_TAG, "Unknown dialog ID " + id);
				dialog = null;
		}
		return dialog;
	}


	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {

		switch (id) {
			case DIALOG_PROBE_RUN_LOG:

				AlertDialog ad = (AlertDialog)dialog;

				ProbeRunStatus status = probeRunForLogDialog.status;

				TextView txtStatusSummary = (TextView)ad.findViewById(R.id.probeRun_log_status_summary);
				TextView txtLog = (TextView)ad.findViewById(R.id.probeRun_log_logText);

				String summary = status.getKeyForDisplay(this) + ": " + probeRunForLogDialog.runSummary;
				txtStatusSummary.setText(summary);
				txtStatusSummary.setBackgroundResource(status.colorResId);

				if(StringUtils.isBlank(probeRunForLogDialog.logText)){
					txtLog.setText(R.string.filler_probe_nologdata);
				}
				else {
					txtLog.setText(probeRunForLogDialog.logText);
				}


			break;

		default:
			Log.e(LOG_TAG, "Unknown dialog ID " + id);

		}
	}
}

