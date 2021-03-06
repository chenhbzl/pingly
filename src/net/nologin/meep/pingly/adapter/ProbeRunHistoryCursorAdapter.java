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
package net.nologin.meep.pingly.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import net.nologin.meep.pingly.PinglyConstants;
import net.nologin.meep.pingly.R;
import net.nologin.meep.pingly.model.ProbeRunStatus;
import net.nologin.meep.pingly.util.DBUtils;
import net.nologin.meep.pingly.util.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static net.nologin.meep.pingly.db.PinglyDataHelper.TBL_PROBE_RUN;

/**
 * cursor adapter to populate probe history entries using a custom layout
 */
public class ProbeRunHistoryCursorAdapter extends SimpleCursorAdapter {

	private LayoutInflater inflater;
	private static final int HISTORY_ITEM_LAYOUT = R.layout.probe_run_history_item;
	private static final String FROM[] = {};
	private static final int TO[] = {};

	public ProbeRunHistoryCursorAdapter(Context context, Cursor c) {
		super(context, HISTORY_ITEM_LAYOUT, c, FROM, TO);
		this.inflater = LayoutInflater.from(context);
	}

	class ViewHolder {

		final DateFormat dateFormatter, timeFormatter;
		public ViewHolder(){
			dateFormatter = new SimpleDateFormat(PinglyConstants.FMT_DAY_DATE_DISPLAY);
			timeFormatter = new SimpleDateFormat(PinglyConstants.FMT_24HR_MIN_SEC_TZ_DISPLAY);
		}

		TextView txtDate, txtTime, txtRunType, txtTimeTaken, txtStatus, txtSummary;

		int colStartTimeIdx, colEndTimeIdx, colScheduleEntryIdx, colStatusIdx, colRunSummaryIdx;

	}

	// http://stackoverflow.com/questions/3535074/getview-vs-bindview-in-a-custom-cursoradapter
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {

		View newView = inflater.inflate(HISTORY_ITEM_LAYOUT, parent, false);

		ViewHolder holder = new ViewHolder();
		// view refs
		holder.txtDate = (TextView) newView.findViewById(R.id.probeRun_item_date);
		holder.txtTime = (TextView) newView.findViewById(R.id.probeRun_item_time);
		holder.txtRunType = (TextView) newView.findViewById(R.id.probeRun_item_runType);
		holder.txtTimeTaken = (TextView) newView.findViewById(R.id.probeRun_item_timetaken);
		holder.txtStatus = (TextView) newView.findViewById(R.id.probeRun_item_status);
		holder.txtSummary = (TextView) newView.findViewById(R.id.probeRun_item_summary);


		// column indexes - see ProbeRunDAO.queryForProbeRunHistoryCursorAdapter() for query
		holder.colStartTimeIdx = cursor.getColumnIndex(TBL_PROBE_RUN.COL_STARTTIME);
		holder.colEndTimeIdx = cursor.getColumnIndex(TBL_PROBE_RUN.COL_ENDTIME);
		holder.colScheduleEntryIdx = cursor.getColumnIndex(TBL_PROBE_RUN.COL_SCHEDULEENTRY_FK);
		holder.colStatusIdx = cursor.getColumnIndex(TBL_PROBE_RUN.COL_STATUS);
		holder.colRunSummaryIdx = cursor.getColumnIndex(TBL_PROBE_RUN.COL_RUN_SUMMARY);

		newView.setTag(holder);

		// bindView will be called after newView, populate items there

		return newView;
	}

	@Override
	public void bindView(View convertView, final Context ctx, Cursor cursor) {

		ViewHolder holder = (ViewHolder) convertView.getTag();

		Date startTime = null, endTime = null;
		long scheduleEntryId = -1;

		if(!cursor.isNull(holder.colStartTimeIdx)){
			startTime = DBUtils.fromGMTDateTimeString(cursor.getString(holder.colStartTimeIdx));
		}
		if(!cursor.isNull(holder.colEndTimeIdx)){
			endTime = DBUtils.fromGMTDateTimeString(cursor.getString(holder.colEndTimeIdx));
		}
		if(!cursor.isNull(holder.colScheduleEntryIdx)){
			scheduleEntryId = cursor.getInt(holder.colScheduleEntryIdx);
		}
		// should never be null
		ProbeRunStatus status = ProbeRunStatus.fromKey(cursor.getString(holder.colStatusIdx));
		String summary = cursor.getString(holder.colRunSummaryIdx);

		String timeTaken = "";
		if(startTime != null && endTime != null){

			long diffMs = endTime.getTime() - startTime.getTime();

			if(diffMs >= 1000){
				timeTaken = ctx.getString(R.string.unit_second_fmt, diffMs / 1000f);
			}
			else{

				timeTaken = ctx.getString(R.string.unit_millis_fmt, diffMs);
			}
		}

		holder.txtDate.setText(startTime == null
				? ctx.getString(R.string.filler_no_date) : holder.dateFormatter.format(startTime));
		holder.txtTime.setText(startTime == null
				? ctx.getString(R.string.filler_no_time) : holder.timeFormatter.format(startTime));
		holder.txtRunType.setText(scheduleEntryId > 0
				? ctx.getString(R.string.probe_history_list_runType_scheduled)
				: ctx.getString(R.string.probe_history_list_runType_manual));
		holder.txtTimeTaken.setText(timeTaken);
		holder.txtStatus.setText(status.getKeyForDisplay(ctx));
		holder.txtStatus.setBackgroundResource(status.colorResId);
		holder.txtSummary.setText(StringUtils.isBlank(summary)
				? ctx.getString(R.string.filler_no_notes) : summary);

	}

}
