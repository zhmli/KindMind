package com.sunyata.kindmind.WidgetAndNotifications;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.sunyata.kindmind.R;
import com.sunyata.kindmind.Utils;

/*
 * Overview: WidgetProviderC contains the onUpdate method which remotely updates the views that are contained
 *  in the (for example) home screen process
 * Details: Because of security considerations Android uses RemoteViews to update widget views
 * Extends: AppWidgetProvider
 * Used in: 
 * Uses app internal: 
 * Uses Android lib: 
 * Notes: 
 * Improvements: 
 * Documentation: 
 *  http://developer.android.com/guide/topics/appwidgets/index.html
 *  https://developer.android.com/reference/android/appwidget/AppWidgetProvider.html
 *  http://docs.eoeandroid.com/resources/samples/ApiDemos/src/com/example/android/apis/appwidget/index.html
 *  Reto's book chapter 14
 */
public class WidgetProviderC extends AppWidgetProvider {
	
	/**
	 * 
	 * Notes:
	 * + When onUpdate is called this will remove the problem with an unresponsive list (for clicking)
	 * 
	 * Can be called/triggered in these ways:
	 * + notifyAppWidgetViewDataChanged (there is a Utils method for this)
	 * + 
	 * + 
	 */
	@Override
	public void onUpdate(Context inContext, AppWidgetManager inAppWidgetManager, int[] inAppWidgetIds){
		Log.d(Utils.getAppTag(), Utils.getMethodName());
		
		//Going through all widgets placed (could be more than one)
		for(int i = 0; i < inAppWidgetIds.length; i++){
			//Setting up the remote view service
			Intent tmpRVServiceIntent = new Intent(inContext, RemoteViewsServiceC.class);
			tmpRVServiceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, inAppWidgetIds[i]);
			tmpRVServiceIntent.setData(Uri.parse(tmpRVServiceIntent.toUri(Intent.URI_INTENT_SCHEME)));
			
			//Setting up the remote views
			RemoteViews tmpRemoteViews = new RemoteViews(inContext.getPackageName(), R.layout.widget);
			tmpRemoteViews.setRemoteAdapter(R.id.widget_listview, tmpRVServiceIntent);
			tmpRemoteViews.setEmptyView(R.id.widget_listview, R.id.widget_empty_view);
			
			//Setting up the pending intent templates
			Intent tmpTemplateIntent = new Intent(inContext, LauncherServiceC.class);
			//-the id will be filled in later in RemoteViewsFactoryC.getViewAt()
			tmpTemplateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, inAppWidgetIds[i]);
			PendingIntent tmpPendingIntent = PendingIntent.getService(
					inContext, 0, tmpTemplateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			tmpRemoteViews.setPendingIntentTemplate(R.id.widget_listview, tmpPendingIntent);

			//Applying the update for the views
			inAppWidgetManager.updateAppWidget(inAppWidgetIds[i], tmpRemoteViews);
		}
	}
}