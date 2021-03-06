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
package net.nologin.meep.pingly.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import net.nologin.meep.pingly.PinglyPrefs;
import net.nologin.meep.pingly.model.ProbeRun;
import net.nologin.meep.pingly.model.ProbeRunStatus;
import net.nologin.meep.pingly.model.ScheduleEntry;
import net.nologin.meep.pingly.model.probe.Probe;
import net.nologin.meep.pingly.util.DBUtils;

import java.util.Date;

import static net.nologin.meep.pingly.db.PinglyDataHelper.TBL_PROBE_RUN;
import static net.nologin.meep.pingly.PinglyConstants.LOG_TAG;

/**
 * All ProbeRun-related data-persistence routines
 */
public class ProbeRunDAO extends PinglyDAO {

    public ProbeRunDAO(PinglyDataHelper dataHelper) {
        super(dataHelper);
    }


    public ProbeRun findProbeRunById(long id) {

		Log.d(LOG_TAG, "Looking up probe run with ID: " + id);

		SQLiteDatabase db = getReadableDB();
		String idClause = TBL_PROBE_RUN.COL_ID + "=" + id;

		Cursor cursor = db.query(TBL_PROBE_RUN.TBL_NAME, null,
				idClause, null, null, null, null);
		if (!cursor.moveToFirst()) {
			Log.d(LOG_TAG, "No probe run found for ID: " + id);
			return null;
		}
		ProbeRun log = cursorToProbeRun(cursor);
		cursor.close();

		return log;
	}

	public void deleteHistoryForProbe(long probeId, boolean finishedProbesOnly) {

		Log.d(LOG_TAG, "Deleting entries for probe " + probeId);

		SQLiteDatabase db = getWriteableDB();
		String whereClause = TBL_PROBE_RUN.COL_PROBE_FK + "=" + probeId;

		if(finishedProbesOnly){
			whereClause += " AND " + TBL_PROBE_RUN.COL_ENDTIME + " IS NOT NULL";
		}

		db.delete(TBL_PROBE_RUN.TBL_NAME, whereClause, null);

	}

	public int pruneHistoryForProbe(long probeId) {

		int numToKeep = PinglyPrefs.getProbeHistorySize(getDataHelperContext());

		Log.d(LOG_TAG, "Pruning probe history, keeping newest " + numToKeep + " entries");

		// the nested IN is required here because android's SQLlite impl doesn't
		// have LIMIT support for the outer DELETE query, so we do it on the inner select

		String sql =
				// delete entries for this probe which are finished
				TBL_PROBE_RUN.COL_PROBE_FK + "=? AND "
				+ TBL_PROBE_RUN.COL_ENDTIME + " IS NOT NULL AND "

				// except for
				+ TBL_PROBE_RUN.COL_ID + " NOT IN ( "

					// the following N most recent, finished runs for this probe
					+ "SELECT " + TBL_PROBE_RUN.COL_ID
					+ " FROM " + TBL_PROBE_RUN.TBL_NAME
					+ " WHERE " + TBL_PROBE_RUN.COL_PROBE_FK + "=? "
					+ " AND " + TBL_PROBE_RUN.COL_ENDTIME + " IS NOT NULL "
					+ " ORDER BY " + TBL_PROBE_RUN.COL_ENDTIME + " DESC "
					+ " LIMIT ?"

				+ " )";

		String[] args = new String[]{
				String.valueOf(probeId), // outer query
				String.valueOf(probeId), // inner select
				String.valueOf(numToKeep) // limit
		};

		Log.d(LOG_TAG, "SQL: " + sql);

		SQLiteDatabase db = getWriteableDB();
		return db.delete(TBL_PROBE_RUN.TBL_NAME, sql, args);

	}

	public Cursor queryForProbeRunHistoryCursorAdapter(Long probeId) {

		Log.d(LOG_TAG, "queryForProbeRunHistoryCursorAdapter (probeId=" + probeId + ")");

		// See ProbeRunHistoryCursorAdapter.newView() for column use

		SQLiteDatabase db = getReadableDB();

		String idClause = null;
		if (probeId != null) {
			idClause = TBL_PROBE_RUN.COL_PROBE_FK + "=" + probeId;
		}

		String orderBy = TBL_PROBE_RUN.COL_ENDTIME + " DESC";

		// a simple query on the run history table will do here
		return db.query(TBL_PROBE_RUN.TBL_NAME, null, idClause, null, null, null, orderBy);

	}


	public ProbeRun prepareNewProbeRun(Probe probe, ScheduleEntry entry) {

		ProbeRun probeRun = new ProbeRun(probe, entry);
		probeRun.id = saveProbeRun(probeRun, false);
		return probeRun;
	}

	public long saveProbeRun(ProbeRun probeRun, boolean pruneAfterSave) {

		Log.d(LOG_TAG, "Saving probe run " + probeRun);

		ScheduleEntry entry = probeRun.scheduleEntry;
		Date start = probeRun.startTime;
		Date end = probeRun.endTime;

		ContentValues cv = new ContentValues();
		cv.put(TBL_PROBE_RUN.COL_PROBE_FK, probeRun.probe.id);
		cv.put(TBL_PROBE_RUN.COL_SCHEDULEENTRY_FK, entry == null ? null : entry.id);
		cv.put(TBL_PROBE_RUN.COL_STARTTIME, start == null ? null : DBUtils.toGMTDateTimeString(start));
		cv.put(TBL_PROBE_RUN.COL_ENDTIME, end == null ? null : DBUtils.toGMTDateTimeString(end));
		cv.put(TBL_PROBE_RUN.COL_STATUS, probeRun.status.getKey());
		cv.put(TBL_PROBE_RUN.COL_RUN_SUMMARY, probeRun.runSummary);
		cv.put(TBL_PROBE_RUN.COL_LOGTEXT, probeRun.logText);

		SQLiteDatabase db = getWriteableDB();

		long affectedId;
		if (probeRun.isNew()) {
			// id should be automatically generated
			affectedId = db.insertOrThrow(TBL_PROBE_RUN.TBL_NAME, null, cv);
		} else {
			String whereClause = TBL_PROBE_RUN.COL_ID + "=" + probeRun.id;
			db.update(TBL_PROBE_RUN.TBL_NAME, cv, whereClause, null);
			affectedId = probeRun.id;
		}

		if(pruneAfterSave){
			// maintain history max size
			this.pruneHistoryForProbe(probeRun.probe.id);
		}

		return affectedId;
	}


	private ProbeRun cursorToProbeRun(Cursor c) {

		CursorReader cr = new CursorReader(c);

		int probeFk = cr.getInt(TBL_PROBE_RUN.COL_PROBE_FK);
        SQLiteDatabase db = getReadableDB();
		Probe probe = ProbeDAO.findProbeById(db, probeFk);

		// schedule will be null if it was a manual run
		int scheduleFk = cr.getInt(TBL_PROBE_RUN.COL_SCHEDULEENTRY_FK, -1);
		ScheduleEntry schedule = null;
		if (scheduleFk > 0) {
			schedule = ScheduleDAO.findById(db, scheduleFk);
		}

		ProbeRun probeRun = new ProbeRun(probe, schedule);
		probeRun.id = cr.getInt(TBL_PROBE_RUN.COL_ID);
		probeRun.startTime = cr.getDate(TBL_PROBE_RUN.COL_STARTTIME, false);
		probeRun.endTime = cr.getDate(TBL_PROBE_RUN.COL_ENDTIME, false);
		probeRun.runSummary = cr.getString(TBL_PROBE_RUN.COL_RUN_SUMMARY);
		probeRun.logText = cr.getString(TBL_PROBE_RUN.COL_LOGTEXT);

		String statusKey = cr.getString(TBL_PROBE_RUN.COL_STATUS);
		probeRun.status = ProbeRunStatus.fromKey(statusKey);

		return probeRun;
	}

}
