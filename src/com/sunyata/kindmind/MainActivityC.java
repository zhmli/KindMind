package com.sunyata.kindmind;

import java.util.Calendar;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import com.sunyata.kindmind.Database.ContentProviderM;
import com.sunyata.kindmind.Database.ItemTableM;
import com.sunyata.kindmind.Database.PatternsTableM;
import com.sunyata.kindmind.List.ListFragmentC;
import com.sunyata.kindmind.List.ListTypeM;
import com.sunyata.kindmind.List.SortingAlgorithmServiceM;

/*
 * Overview: MainActivityC holds three ListFragments in a ViewPager and handles the corresponding tabs
 * Sections:
 * ------------------------Fields
 * ------------------------onCreate and OnPageChangeListener
 * ------------------------Pager adapter
 * ------------------------Callback methods
 * ------------------------Other methods
 * Improvements: Saving the view pager position in a bundle instead of a static variable
 * Notes: In various places in this class a check is made before calling setCurrentItem for the ViewPager,
 *  for example: "if(mViewPager.getCurrentItem() != tmpPos){". The reason for this is what is an Android bug
 *  which makes action bar items invisible because of a race condition and therefore we remove unnecessary
 *  calls to setCurrentItem. For more info, please see the following links:
 *  http://stackoverflow.com/questions/13998473/disappearing-action-bar-buttons-when-swiping-between-fragments
 *  http://code.google.com/p/android/issues/detail?id=29472
 * Documentation: 
 *  http://developer.android.com/training/implementing-navigation/lateral.html
 *  http://developer.android.com/reference/android/support/v4/app/FragmentActivity.html
 *  http://developer.android.com/reference/android/support/v4/view/ViewPager.html
 */
public class MainActivityC extends FragmentActivity implements MainActivityCallbackListenerI{

	//------------------------Fields
	
	//Fragment changes
    private ViewPager mViewPager;
	private FragmentStatePagerAdapterM mPagerAdapter;
    private static int sViewPagerPosition;

    //Action bar
    private ActionBar refActionBar;
    private String mFeelingTitle;
    private String mNeedTitle;
    private String mActionTitle;
    
    public final static String EXTRA_URI_AS_STRING = "uri_as_string";
    
    //------------------------onCreate and OnPageChangeListener
    
    /*
	 * Overview: onCreate does fundamental setup for the app, including an OnPageChangeListener,
	 *  a TabListener and creation of the startup list items
	 * Notes: This method may be called not only at the start of the application but also later
	 *  to recreate the activity, for example after coming back from a details screen.
	 * Documentation: 
	 *  http://developer.android.com/reference/android/support/v4/view/ViewPager.OnPageChangeListener.html
	 */
    @Override
    protected void onCreate(Bundle inSavedInstanceState) {
        super.onCreate(inSavedInstanceState);
        Log.d(Utils.getClassName(), Utils.getMethodName());
        
        //Activating strict mode for debug builds
        // More info: http://developer.android.com/reference/android/os/StrictMode.html
        /*
        if(Utils.BuildConfig.DEBUG){
        	StrictMode.enableDefaults();
        }
        */
        
    	//Creation of new list items
    	if(Utils.isFirstTimeApplicationStarted(this) == true){
    		Utils.createAllStartupItems(this);
    	}

    	//Setting layout and title
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);
        
        //Create the adapter that will return a fragment for each section of the app
        mPagerAdapter = new FragmentStatePagerAdapterM(getSupportFragmentManager());

        //Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOffscreenPageLimit(0);
        //-Using this becase getAdapter sometimes gives null, for more info, see this link:
        // http://stackoverflow.com/questions/13651262/getactivity-in-arrayadapter-sometimes-returns-null

        //Create and set the OnPageChangeListener for the ViewPager
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			//-To access one fragment from here we can use this line:
			// ((CustomPagerAdapter)mViewPager.getAdapter()).getItem(pos).refreshListDataSupport();
			@Override
			public void onPageSelected(int inPos) { //[list update]
				Log.d("ViewPager.OnPageChangeListener()", "onPageSelected()");
				
				//Resetting the sorting
				Utils.setItemTableSortType(SortTypeM.KINDSORT);
				
				//Setting the active tab when the user has just side scrolled (swiped) to a new fragment
				getActionBar().setSelectedNavigationItem(inPos);
			}
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}
			@Override
			public void onPageScrollStateChanged(int inState) {
				switch(inState){
				case ViewPager.SCROLL_STATE_IDLE:
					//Saving the position (solves the problem in issue #41)
					sViewPagerPosition = mViewPager.getCurrentItem();
					break;
				default:
					break;
				}
			}
		});

        //Setup of actionbar with tabs
        refActionBar = this.getActionBar();
        refActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.TabListener tmpTabListener = new ActionBar.TabListener() {
			@Override
			public void onTabSelected(Tab tab, FragmentTransaction ft) {
				//Scrolling to the new fragment when the user selects a tab
				int tmpPos = tab.getPosition();
				
				if(mViewPager.getCurrentItem() != tmpPos){
					mViewPager.setCurrentItem(tmpPos);
				}
			}
			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			}
			@Override
			public void onTabReselected(Tab tab, FragmentTransaction ft) {
			}
        };
        refActionBar.addTab(refActionBar.newTab().setText(mFeelingTitle).setTabListener(tmpTabListener));
        refActionBar.addTab(refActionBar.newTab().setText(mNeedTitle).setTabListener(tmpTabListener));
        refActionBar.addTab(refActionBar.newTab().setText(mActionTitle).setTabListener(tmpTabListener));
        this.updateTabTitles();
        
        
        
        
        
    	//Extracting data from the intent given when calling this activity (used by widgets and notifications)
    	if(this.getIntent() != null && this.getIntent().hasExtra(EXTRA_URI_AS_STRING)){
    		String tmpExtraFromString = this.getIntent().getStringExtra(EXTRA_URI_AS_STRING);
        	Uri tmpItemUri = Uri.parse(tmpExtraFromString);
        	if(tmpItemUri != null){
            	this.clearAllActiveInDatabase();
            	
            	//Updating the db value
            	ContentValues tmpContentValues = new ContentValues();
            	tmpContentValues.put(ItemTableM.COLUMN_ACTIVE, 1);
            	getContentResolver().update(tmpItemUri, tmpContentValues, null, null);
            	
            	///SortingAlgorithmM.get(this).updateSortValuesForListType();
            	this.startService(new Intent(this, SortingAlgorithmServiceM.class));
            	
            	this.updateTabTitles();

            	//Setting up the cursor and extracting the list type..
            	Cursor tmpItemCur = getContentResolver().query(tmpItemUri, null, null, null, null);
            	tmpItemCur.moveToFirst();
            	int tmpListType = tmpItemCur.getInt(tmpItemCur.getColumnIndexOrThrow(ItemTableM.COLUMN_LIST_TYPE));
            	tmpItemCur.close();
            	
            	//..setting the Viewpager position
            	if(mViewPager.getCurrentItem() != tmpListType){
            		sViewPagerPosition = tmpListType;
        			mViewPager.setCurrentItem(sViewPagerPosition);
        		}
        	}
    	}
    	
    	
    	
    }
    

	//------------------------Pager adapter
    
    /*
	 * Overview: PagerAdapterM handles the listfragments that makes up the core of the app
	 * Used in: In onCreate setAdapater is called: "mViewPager.setAdapter(mPagerAdapter);"
	 * PLEASE NOTE: Was previously a FragmentPagerAdapter, but this resulted in bugs (one of which was outside
	 *  of the app code) so we use FragmentStatePagerAdapter instead even though the docs recommend the other
	 *  for cases where we have only a few tabs as in our case
	 * Documentation:
	 *  http://developer.android.com/reference/android/support/v4/app/FragmentStatePagerAdapter.html
	 */
    class FragmentStatePagerAdapterM extends FragmentStatePagerAdapter {
        private ListFragmentC mFeelingListFragment;
        private ListFragmentC mNeedListFragment;
        private ListFragmentC mKindnessListFragment;
        public FragmentStatePagerAdapterM(FragmentManager inFragmentManager) {
            super(inFragmentManager);
        }
        @Override
        public Object instantiateItem (ViewGroup inContainer, int inPosition){
        	switch(inPosition){
        	case ListTypeM.FEELINGS:
        		mFeelingListFragment = ListFragmentC.newInstance(ListTypeM.FEELINGS,
        				(MainActivityCallbackListenerI)MainActivityC.this);
        		break;
        	case ListTypeM.NEEDS:
           		mNeedListFragment = ListFragmentC.newInstance(ListTypeM.NEEDS,
        				(MainActivityCallbackListenerI)MainActivityC.this);
        		break;
        	case ListTypeM.KINDNESS:
           		mKindnessListFragment = ListFragmentC.newInstance(ListTypeM.KINDNESS,
        				(MainActivityCallbackListenerI)MainActivityC.this);
        		break;
        	case ListTypeM.NOT_SET:
        	default:
        		Log.e(Utils.getClassName(), "Error in instantiateItem: Case not covered or not set");
        		break;
        	}
        	return super.instantiateItem(inContainer, inPosition);
        }
        @Override
        public ListFragmentC getItem(int inPosition) {
        	switch (inPosition){
		    	case ListTypeM.FEELINGS:	return mFeelingListFragment;
				case ListTypeM.NEEDS:		return mNeedListFragment;
		    	case ListTypeM.KINDNESS:	return mKindnessListFragment;
		    	case ListTypeM.NOT_SET:
		    	default:
		    		Log.e(Utils.getClassName(), "Error in method getItem: case not covered or not set");
		    		return null;
        	}
        }
        @Override
        public int getCount() {
            return ListTypeM.NUMBER_OF_TYPES;
        }
    }

    
    //------------------------Callback methods

    /*
	 * Overview: fireSavePatternEvent saves as a pattern all the currently checked list items
	 * Used in: ListFragmentC when the user presses the save button menu item
	 * Uses app internal: fireClearAllListsEvent
	 */
	@Override
	public void fireSavePatternEvent() {
		Cursor tmpItemCur = this.getContentResolver().query(
				ContentProviderM.ITEM_CONTENT_URI, null, null, null, ContentProviderM.sSortType);
		
		long tmpCurrentTime = Calendar.getInstance().getTimeInMillis();
		//-getting the time here instead of inside the for statement ensures that we are able
		// to use the time as way to group items into a pattern.
		
		//Iterate through the list items to find the ones that are checked/active..
		for(tmpItemCur.moveToFirst(); tmpItemCur.isAfterLast() == false; tmpItemCur.moveToNext()){
			if(Utils.sqlToBoolean(tmpItemCur, ItemTableM.COLUMN_ACTIVE)){
				//..saving to pattern in database
				ContentValues tmpInsertContentValues = new ContentValues();
				long tmpItemId = tmpItemCur.getInt(tmpItemCur.getColumnIndexOrThrow(ItemTableM.COLUMN_ID));
				tmpInsertContentValues.put(PatternsTableM.COLUMN_ITEM_REFERENCE, tmpItemId);
				tmpInsertContentValues.put(PatternsTableM.COLUMN_CREATE_TIME, tmpCurrentTime);
				this.getContentResolver().insert(ContentProviderM.PATTERNS_CONTENT_URI, tmpInsertContentValues);
			}
		}
		Toast.makeText(this, "KindMind pattern saved", Toast.LENGTH_LONG).show();
		
		//Limiting the number of rows in the patterns table
		this.limitPatternsTable();
		
		//Clearing data and updating the gui
		fireClearDatabaseAndUpdateGuiEvent();
		
		tmpItemCur.close();
	}
	/*
	 * Overview: limitPatternsTable removes zero or more patterns, keeping the pattern table (1) relevant and
	 *  (2) at a lenght which does not take too much resources for the sorting algorithm
	 * Used in: fireSavePatternEvent
	 * Notes: 1. We limit the pattern table based on the number of rows (and not the number of patterns).
	 * 2. We expect the while loop to be run completely only one time on average since this method is called from
	 *  the same method that adds new patterns (if we have just added a very large pattern it may be run many times)
	 * 3. The only reason that a for loop is used is so that in case of some error with deletion from the database
	 *  we don't get stuck in an infinite loop.
	 * Improvements: 
	 */
	private void limitPatternsTable(){
		
		Cursor tmpPatternsCur = null;
		final int WARNING_LIMIT = 100;

		for(int i = 0; i < WARNING_LIMIT; i++){
			//Sorting "by pattern" (by create time)
			tmpPatternsCur = this.getContentResolver().query(
					ContentProviderM.PATTERNS_CONTENT_URI, null, null, null,
					PatternsTableM.COLUMN_CREATE_TIME + " ASC");
			
			//Looping until we are on or under the max limit or rows
			if(tmpPatternsCur.getCount() <= Utils.getMaxNumberOfPatternRows()){
				tmpPatternsCur.close();
				return;
			}
			
			//Extracting the first (and oldest) time entry
			tmpPatternsCur.moveToFirst();
			long tmpFirstTimeEntry = tmpPatternsCur.getLong(
					tmpPatternsCur.getColumnIndexOrThrow(PatternsTableM.COLUMN_CREATE_TIME));
			
			//Using the first time entry as a selection value for removing all rows for this whole pattern from the db
			String tmpSelection = PatternsTableM.COLUMN_CREATE_TIME + "=" + tmpFirstTimeEntry;
			this.getContentResolver().delete(ContentProviderM.PATTERNS_CONTENT_URI, tmpSelection, null);
			
			tmpPatternsCur.close();
		}
		
		//If we get here it means that we have looped more than the "warning limit" which is an indication that
		// something has gone wrong
		Log.w(Utils.getClassName(),
				"Warning in limitPatternsTable: Number of iterations has reached " + WARNING_LIMIT
				+ ", exiting method");
	}
	
	@Override
	public void fireClearDatabaseAndUpdateGuiEvent() {
		this.clearAllActiveInDatabase();
		this.startService(new Intent(this, SortingAlgorithmServiceM.class));
		this.updateTabTitles();
		this.scrollLeftmost();
	}

	/*
	 * Overview: fireClearAllListsEvent clears all marks for checked/activated list items
	 * Used in:
	 * Improvements:
	 */
	private void clearAllActiveInDatabase() { //[list update]
		//Clearing all the checks for all list items
		ContentValues tmpContentValueForUpdate = new ContentValues();
		tmpContentValueForUpdate.put(ItemTableM.COLUMN_ACTIVE, ItemTableM.FALSE);
		Uri tmpUri = Uri.parse(ContentProviderM.ITEM_CONTENT_URI.toString());
		this.getContentResolver().update(tmpUri, tmpContentValueForUpdate, null, null);
	}
	
	private void scrollLeftmost(){
		//Side scrolling to the leftmost viewpager position (feelings)
		if(mViewPager.getCurrentItem() != 0){
			mViewPager.setCurrentItem(0, true);
		}
	}
	
	/*
	 * Overview: fireUpdateTabTitles updates tab titles with the name of the listtype and - if one or more
	 *  list items have been checked/activated - adds the number of checks for that list type/fragment
	 * Used in: 1. fireSavePatternEvent 2. ListFragmentC.onListItemClick() 3. onCreate
	 */
	private void updateTabTitles() {
		mFeelingTitle = getResources().getString(R.string.feelings_title);
        mNeedTitle = getResources().getString(R.string.needs_title);
        mActionTitle = getResources().getString(R.string.kindness_title);
        int tmpFeelingsCount = Utils.getActiveListItemCount(this, ListTypeM.FEELINGS);
        int tmpNeedsCount = Utils.getActiveListItemCount(this, ListTypeM.NEEDS);
        int tmpActionsCount = Utils.getActiveListItemCount(this, ListTypeM.KINDNESS);
        if(tmpFeelingsCount != 0){mFeelingTitle = mFeelingTitle + " (" + tmpFeelingsCount + ")";}
        if(tmpNeedsCount != 0){mNeedTitle = mNeedTitle + " (" + tmpNeedsCount + ")";}
        if(tmpActionsCount != 0){mActionTitle = mActionTitle + " (" + tmpActionsCount + ")";}
        refActionBar.getTabAt(0).setText(mFeelingTitle);
        refActionBar.getTabAt(1).setText(mNeedTitle);
        refActionBar.getTabAt(2).setText(mActionTitle);
	}

    /*
	 * Overview: resetData clears and repopulates the list of data items. Used for testing and debug purposes
	 */
    public void fireResetData(){
    	//Clearing the data
    	this.getContentResolver().delete(ContentProviderM.ITEM_CONTENT_URI, null, null);
    	this.getContentResolver().delete(ContentProviderM.PATTERNS_CONTENT_URI, null, null);
    	
    	//Adding new data
    	Utils.createAllStartupItems(this);
    }

	
    //------------------------Other methods
    
    @Override
    public void onResume(){
    	super.onResume();
    	Log.d(Utils.getClassName(), Utils.getMethodName());
    	
    	if(sViewPagerPosition != mViewPager.getCurrentItem()){
    		mViewPager.setCurrentItem(sViewPagerPosition);
    		//-solves the problem in issue #41
    	}
    }
    
}
