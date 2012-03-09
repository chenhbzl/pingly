package net.nologin.meep.pingly.activity;

import net.nologin.meep.pingly.R;
import net.nologin.meep.pingly.adapter.PinglyCursorProbeAdapter;
import net.nologin.meep.pingly.model.ProbeDataHelper;
import net.nologin.meep.pingly.model.Probe;

import static net.nologin.meep.pingly.PinglyConstants.LOG_TAG;

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
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

public class ProbeListActivity extends BasePinglyActivity {

	private ProbeDataHelper data;
	private PinglyCursorProbeAdapter listAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.probe_list);

		data = new ProbeDataHelper(this);

		Cursor allProbesCursor = data.findAllProbes();
		listAdapter = new PinglyCursorProbeAdapter(this,allProbesCursor);
		ListView lv = (ListView) findViewById(R.id.probeList);
		lv.setAdapter(listAdapter);
		registerForContextMenu(lv);

		View empty = findViewById(R.id.emptyListElem);	    
	    lv.setEmptyView(empty);

//	    int[] colors = {0, 0xFF007700, 0}; //green
//	    lv.setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, colors));
//	    lv.setDividerHeight(1);

		// any activity 
		startManagingCursor(allProbesCursor);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int itemPos, long itemId) {
                Log.d(LOG_TAG,"Got id " + itemId);
                goToProbeRunner(itemId);
            }
        });

		// ======================== test stuff start ==========================
		// add button to quickly generate items
		ImageButton testItemsBut = (ImageButton) findViewById(R.id.but_generateTestItems);
		testItemsBut.setOnClickListener(new OnClickListener() {			
			public void onClick(View v) {
	
				AlertDialog dialog = new AlertDialog.Builder(ProbeListActivity.this)
				.setMessage("Generate Test Items?")
				.setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			       public void onClick(DialogInterface dialog, int id) {
						data.generateTestItems();
						listAdapter.changeCursor(data.findAllProbes());
			       }
			   })
			   .setNegativeButton("No", new DialogInterface.OnClickListener() {
			       public void onClick(DialogInterface dialog, int id) {
			            dialog.cancel();
			       }
			   }).create();
			dialog.show();						
			}
		});
		
		// add button to quickly generate items
		ImageButton deleteAllBut = (ImageButton) findViewById(R.id.but_deleteAllItems);
		deleteAllBut.setOnClickListener(new OnClickListener() {			
			public void onClick(View v) {
	
				AlertDialog dialog = new AlertDialog.Builder(ProbeListActivity.this)
				.setMessage("Are you sure you want to delete all items?")
				.setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			       public void onClick(DialogInterface dialog, int id) {
						data.deleteAll();
						listAdapter.changeCursor(data.findAllProbes());
			       }
			   })
			   .setNegativeButton("No", new DialogInterface.OnClickListener() {
			       public void onClick(DialogInterface dialog, int id) {
			            dialog.cancel();
			       }
			   }).create();
			dialog.show();						
			}
		});
		
		
		// ======================== temp stuff end ==========================
	}

	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (data != null) {
			data.close();
		}
	}


	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);

		if (v.getId() == R.id.probeList) {

			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

			Probe target = data.findProbeById(info.id);
			menu.setHeaderTitle("Probe: '" + target.name + "'");

			MenuInflater inflater1 = getMenuInflater();
			inflater1.inflate(R.menu.probe_list_context, menu);

			
			
			// menu.setHeaderTitle(Countries[info.position]);
			// String[] menuItems =
			// getResources().getStringArray(R.array.probeList_context_menu);
			// for (int i = 0; i < menuItems.length; i++) {
			// menu.add(Menu.NONE, i, i, menuItems[i]);
			// }
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();

		final Probe probe = data.findProbeById(info.id);
		
		switch (item.getItemId()) {

			case R.id.probe_list_contextmenu_run:
				Log.d("PINGLY", "Running probe: " + probe);
				
				goToProbeRunner(probe.id);
				
				return true;
			
			case R.id.probe_list_contextmenu_edit:
                Log.d("PINGLY", "Editing probe: " + probe);
				
				goToProbeDetails(probe.id);
				
				return true;


			case R.id.probe_list_contextmenu_delete:
				Log.d("PINGLY", "Deleting probe: " + probe);
				
				AlertDialog dialog = new AlertDialog.Builder(this)
						.setMessage("Are you sure you want to delete probe '" + probe.name + "'?")
				       .setCancelable(false)
				       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   data.deleteProbe(probe);
								/* since we stay where we are (no activity state change), the startManagingCursor() registration in onCreate()
								 * won't know to refresh the cursor/adapter.  We requery all probes and pass the new cursor to the adapter. */
								listAdapter.changeCursor(data.findAllProbes());
				           }
				       })
				       .setNegativeButton("No", new DialogInterface.OnClickListener() {
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