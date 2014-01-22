package com.sunyata.kindmind.WidgetAndNotifications;

import java.util.Date;

import com.sunyata.kindmind.R;
import com.sunyata.kindmind.Utils;
import com.sunyata.kindmind.Database.ItemTableM;
import com.sunyata.kindmind.Database.KindMindContentProviderM;
import com.sunyata.kindmind.List.MainActivityC;
import com.sunyata.kindmind.R.drawable;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
//-NotificationCompat is for api lvl 15 and downwards

public class NotificationServiceC extends IntentService {

	//-------------------Fields and constructor
	
	private static final String TAG = "NotificationServiceC";
	static final String PREFERENCES_NOTIFICATION_LIST = "NotificationList";
	private static final String NOTIFICATION_UUID = "NotificationUUID";
	private static final String NOTIFICATION_TITLE = "NotificationTitle";

	public NotificationServiceC() {
		super(TAG);
	}

	
	//----------------------------Static methods for setting repeating alarm/alarms
	
	/*
	 * Overview: setServiceNotificationAll iterates through the rows in the database and calls
	 *  setServiceNotificationSingle for each row/listitem that has an active notification
	 * Usage: Only BootCompleteReceiverC.onReceive()
	 * Uses app internal: this.setServiceNotificationSingle()
	 */
	public static void setServiceNotificationAll(Context inContext){
		long tmpNotification = -1;
		Uri tmpItemUri = null;
		
		//Creating SQL cursor
		Cursor tmpCursor = inContext.getContentResolver().query(
				KindMindContentProviderM.LIST_CONTENT_URI, null, null, null, KindMindContentProviderM.sSortType);
		if(tmpCursor.getCount() == 0){
			//tmpCursor.close();
			return;
		}
		
		//Iterating through all the database rows..
		for(tmpCursor.moveToFirst(); tmpCursor.isAfterLast() == false; tmpCursor.moveToNext()){
			
			//..extracting notification data and list item URI
			tmpNotification = tmpCursor.getLong(tmpCursor.getColumnIndexOrThrow(ItemTableM.COLUMN_NOTIFICATION));
			tmpItemUri = Uri.withAppendedPath(
					KindMindContentProviderM.LIST_CONTENT_URI,
					"/" +
					(tmpCursor.getLong(tmpCursor.getColumnIndexOrThrow(ItemTableM.COLUMN_ID))));
			
			//..if the notification is active, calling setServiceNotificationSingle
			if(tmpNotification > -1){
				setServiceNotificationSingle(inContext, tmpItemUri, tmpNotification);
			}
		}
		
		//tmpCursor.close();
	}
	
	
	/*
	 * Overview: setServiceNotificationSingle sets a repeating notification for a single list item
	 * Usage: this.setServiceNotificationAll(), DetailsFragmentC.changeNotificationService()
	 * Uses Android lib: AlarmManager.setRepeating()
	 */
	public static void setServiceNotificationSingle(Context inContext, Uri inItemUri, long inIntervalInMilliSeconds){
		//Setting up an SQL cursor to point to the row for the item URI
		Cursor tmpCursor = inContext.getContentResolver().query(inItemUri, null, null, null, KindMindContentProviderM.sSortType);
		if(tmpCursor.getCount() == 0){
			//tmpCursor.close();
			return;
		}
		tmpCursor.moveToFirst();
		
		//Extracting various data values from the cursor/database-row for use later in this method..
		String tmpItemIdAsString = Long.valueOf(
				tmpCursor.getLong(tmpCursor.getColumnIndexOrThrow(ItemTableM.COLUMN_ID)))
						.toString();
		String tmpItemName = tmpCursor.getString(tmpCursor.getColumnIndexOrThrow(ItemTableM.COLUMN_NAME));
		
		//..extracting time and whether or not notifications are active for this list item
		boolean tmpItemNotificationIsActive = true;
		long tmpItemTimeInMilliSeconds = tmpCursor.getLong(
				tmpCursor.getColumnIndexOrThrow(ItemTableM.COLUMN_NOTIFICATION));
		if(tmpItemTimeInMilliSeconds == -1 ){
			tmpItemNotificationIsActive = false;
		}
		
		//Creation and setup of an Intent pointing to this class which has the onHandleIntent method
		Intent tmpIntent = new Intent(inContext, NotificationServiceC.class);
		tmpIntent.setType(tmpItemIdAsString); //This is what makes the intents differ
		tmpIntent.putExtra(NOTIFICATION_UUID, tmpItemIdAsString);
		tmpIntent.putExtra(NOTIFICATION_TITLE, tmpItemName);

		//Setting the repeating alarm, or cancelling it (depending on database value)
		PendingIntent tmpPendingIntentToRepeat = PendingIntent.getService(
				inContext, 0, tmpIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
		AlarmManager tmpAlarmManager = (AlarmManager)inContext.getSystemService(Context.ALARM_SERVICE);
		if(tmpItemNotificationIsActive == true){
			Log.i(Utils.getClassName(), "date = " + new Date(tmpItemTimeInMilliSeconds));
			tmpAlarmManager.setRepeating(AlarmManager.RTC, tmpItemTimeInMilliSeconds, inIntervalInMilliSeconds,
					tmpPendingIntentToRepeat);
			//-PLEASE NOTE: Initial time inUserTimeInMillseconds is not modified with
			// TimeZone.getDefault().getRawOffset() in spite of the documentation for AlarmManager.RTC which indicates
			// that UTC is used.
		}else{
			tmpAlarmManager.cancel(tmpPendingIntentToRepeat);
			tmpPendingIntentToRepeat.cancel();
		}
		
		//tmpCursor.close();
	}
	
	
	//-------------------Overridden IntentService methods
	
	/*
	 * Overview: onHandleIntent is called at a regular interval set by AlarmManager.setRepeating()
	 *  and shows a notification to the user
	 * Usage: this.setServiceNotificationSingle()
	 * Uses Android libs: NotificationCompat.Builder, NotificationManager
	 * Improvements: 
	 * Documentation: https://developer.android.com/reference/android/app/IntentService.html#onHandleIntent%28android.content.Intent%29
	 */
	@Override
	protected void onHandleIntent(Intent inPendingIntent) {
		Log.d(Utils.getClassName(), "In method onHandleIntent: One intent received");
		
		//Creating the PendingIntent which will be used for the notification
		PendingIntent tmpPendingIntent = PendingIntent.getActivity(
				this, 0, new Intent(this, MainActivityC.class), 0);
		//-Please note: Request code is not used by the class (see the documentation)
		
		//Extract data attached to the intent coming in to this method
		String tmpUuidStringFromListDataItem = inPendingIntent.getStringExtra(NOTIFICATION_UUID);
		String tmpTitleStringFromListDataItem = inPendingIntent.getStringExtra(NOTIFICATION_TITLE);

		//Build the notification..
		Notification tmpNotification = new NotificationCompat.Builder(this)
				.setTicker("Ticker text " + tmpTitleStringFromListDataItem)
				.setSmallIcon(R.drawable.kindmind_icon)
				.setContentTitle(tmpTitleStringFromListDataItem)
				.setContentText(tmpTitleStringFromListDataItem)
				.setContentIntent(tmpPendingIntent)
				.setAutoCancel(true)
				.build();
		
		//..and display it
		NotificationManager tmpNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		tmpNotificationManager.notify(tmpUuidStringFromListDataItem, 0, tmpNotification); //TODO: Change the 0
	}
}