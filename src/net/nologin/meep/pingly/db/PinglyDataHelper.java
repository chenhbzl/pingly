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


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import static net.nologin.meep.pingly.PinglyConstants.LOG_TAG;

/**
 * Data helper for use by DAOs, also provides DB setup/upgrade routines.
 *
 * TODO: change this code and the DAO code to use an ORM library
 * (whenever I find one that isn't huge and seems usable - OrmLite perhaps?)
 */
public final class PinglyDataHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Pingly.db";
    private static final int DATABASE_VERSION = 1;
	private final Context context;

    public PinglyDataHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
    }

	protected Context getDataHelperContext(){
		return context;
	}

    public static final class TBL_PROBE {
        public static final String TBL_NAME = "pingly_probes";

        public static final String COL_ID = BaseColumns._ID;
        public static final String COL_TYPE_KEY = "probe_type_key";
        public static final String COL_NAME = "probe_name";
        public static final String COL_DESC = "desc";
        public static final String COL_CONFIG = "config";
        public static final String COL_CREATED = "t_created";
        public static final String COL_LASTMOD = "t_lastmod";

        public static final String CREATE_SQL = "CREATE TABLE " + TBL_NAME + "( "
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TYPE_KEY + " TEXT NOT NULL, "
                + COL_CREATED + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + COL_LASTMOD + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + COL_NAME + " TEXT NOT NULL UNIQUE, "
                + COL_DESC + " TEXT, "
                + COL_CONFIG + " TEXT )";

    }

	public static final class TBL_SCHEDULE {
		public static final String TBL_NAME = "pingly_schedule";

		public static final String COL_ID = BaseColumns._ID;
        public static final String COL_PROBE_FK = "probe_fk";
		public static final String COL_ACTIVE = "is_active";
		public static final String COL_CREATED = "t_created";
		public static final String COL_LASTMOD = "t_lastmod";
        public static final String COL_STARTONSAVE = "start_on_save";
        public static final String COL_STARTTIME = "start_time";
        public static final String COL_REPEATTYPE_ID = "repeat_type_id";
        public static final String COL_REPEAT_VALUE = "repeat_value";
		public static final String COL_NOTIFY_OPTS = "notify_opts";

        public static final String CREATE_SQL = "CREATE TABLE " + TBL_NAME + "( "
				+ COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_PROBE_FK + " INTEGER NOT NULL, "
                + COL_CREATED + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + COL_LASTMOD + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + COL_ACTIVE + " INTEGER NOT NULL DEFAULT 1, "
                + COL_STARTONSAVE + " INTEGER NOT NULL DEFAULT 1, "
                + COL_STARTTIME + " DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL, "
                + COL_REPEATTYPE_ID + " INTEGER NOT NULL DEFAULT 0,"
                + COL_REPEAT_VALUE + " INTEGER, "
				+ COL_NOTIFY_OPTS + " TEXT, "
                + "FOREIGN KEY(" + COL_PROBE_FK + ") REFERENCES "
                        + TBL_PROBE.TBL_NAME + "(" + TBL_PROBE.COL_ID + ")"
				+ "   ) ";

	}

	public static final class TBL_PROBE_RUN {
		public static final String TBL_NAME = "pingly_proberun";

		public static final String COL_ID = BaseColumns._ID;
		public static final String COL_PROBE_FK = "probe_fk";
		public static final String COL_SCHEDULEENTRY_FK = "scheduleentry_fk";
		public static final String COL_STARTTIME = "start_time";
		public static final String COL_ENDTIME = "end_time";
		public static final String COL_STATUS = "status";
		public static final String COL_RUN_SUMMARY = "run_summary";
		public static final String COL_LOGTEXT = "logtext";

		public static final String CREATE_SQL = "CREATE TABLE " + TBL_NAME + "( "
				+ COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ COL_PROBE_FK + " INTEGER NOT NULL, "
				+ COL_SCHEDULEENTRY_FK + " INTEGER, " // can be null for a manual run!
				+ COL_STARTTIME + " DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL, "
				+ COL_ENDTIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " // null possible, only set on finish
				+ COL_STATUS + " TEXT NOT NULL, "
				+ COL_RUN_SUMMARY + " TEXT , "
				+ COL_LOGTEXT + " TEXT , "
				+ "FOREIGN KEY(" + COL_PROBE_FK + ") REFERENCES "
				+ TBL_PROBE.TBL_NAME + "(" + TBL_PROBE.COL_ID + "), "
				+ "FOREIGN KEY(" + COL_SCHEDULEENTRY_FK + ") REFERENCES "
				+ TBL_SCHEDULE.TBL_NAME + "(" + TBL_SCHEDULE.COL_ID + ")"
				+ "   ) ";


	}

    private static String generateLastModTrigger(String tblName, String idName, String lastModName){

        return " CREATE TRIGGER " + tblName + "_LM_TRIG "
                + "AFTER UPDATE ON " + tblName + " FOR EACH ROW BEGIN "
                + "UPDATE " + tblName + " SET " + lastModName + "=CURRENT_TIMESTAMP "
                + "WHERE " + idName + "=OLD." + idName  + "; END";
        
    }
        
    @Override
    public void onCreate(SQLiteDatabase db) {

		Log.d(LOG_TAG, "Handling onCreate for table: " + TBL_PROBE.TBL_NAME);
        db.execSQL(TBL_PROBE.CREATE_SQL);
        db.execSQL(generateLastModTrigger(TBL_PROBE.TBL_NAME,TBL_PROBE.COL_ID, TBL_PROBE.COL_LASTMOD));

        Log.d(LOG_TAG, "Handling onCreate for table: " + TBL_SCHEDULE.TBL_NAME);
        db.execSQL(TBL_SCHEDULE.CREATE_SQL);
        db.execSQL(generateLastModTrigger(TBL_SCHEDULE.TBL_NAME,TBL_SCHEDULE.COL_ID, TBL_SCHEDULE.COL_LASTMOD));

		Log.d(LOG_TAG, "Handling onCreate for table: " + TBL_PROBE_RUN.TBL_NAME);
		db.execSQL(TBL_PROBE_RUN.CREATE_SQL);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		// initial version of app, this shouldn't be called
        Log.w(LOG_TAG, "onUpgrade called, oldVer=" + oldVersion + ", newVer=" + newVersion);

    }

}
