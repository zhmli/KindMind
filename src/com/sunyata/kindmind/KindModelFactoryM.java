package com.sunyata.kindmind;

import android.content.Context;
import android.util.Log;

import com.sunyata.kindmind.ListDataItemM.ListTypeM;

//Design pattern (honourable mention): Simple factory
public class KindModelFactoryM {

	private Context refContext;
	private ListTypeM refTemporaryListType;
	private ListDataM retTemporaryList;
	
	KindModelFactoryM(Context inContext){
		refContext = inContext;
	}
	
	ListDataM createListData(ListTypeM inListType){
		refTemporaryListType = inListType;
		
		switch(inListType){
		
		case SPECEV:
			retTemporaryList = new ListDataM(ListTypeM.SPECEV, KindModelM.JSON_OBSERVATIONS_SPECEV_FILE_NAME, refContext); //Loads from JSON
			createAndAddDataItem("Thinking that someone has negative intentions");
			return retTemporaryList;

		case SUFFERING:
			retTemporaryList = new ListDataM(ListTypeM.KINDNESS, KindModelM.JSON_FEELINGS_SUFFERING_FILE_NAME, refContext); //Loads from JSON
			createAndAddDataItem("Angry");
			createAndAddDataItem("Annoyed");
			createAndAddDataItem("Concerned");
			createAndAddDataItem("Confused");
			createAndAddDataItem("Dissapointed");
			createAndAddDataItem("Discouraged");
			createAndAddDataItem("Distressed");
			createAndAddDataItem("Embarassed");
			createAndAddDataItem("Frustrated");
			createAndAddDataItem("Helpless");
			createAndAddDataItem("Hopeless");
			createAndAddDataItem("Impatient");
			createAndAddDataItem("Irritated");
			createAndAddDataItem("Lonely");
			createAndAddDataItem("Nervous");
			createAndAddDataItem("Overwhelmed");
			createAndAddDataItem("Puzzled");
			createAndAddDataItem("Reluctant");
			createAndAddDataItem("Sad");
			createAndAddDataItem("Uncomfortable");
			return retTemporaryList;
			
		case NEEDS:
			retTemporaryList = new ListDataM(ListTypeM.KINDNESS, KindModelM.JSON_NEEDS_FILE_NAME, refContext); //Loads from JSON
			createAndAddDataItem("Choosing dreams/goals/values");
			createAndAddDataItem("Choosing plans for fulfilling dreams/goals/values");
			createAndAddDataItem("Celbratation");
			createAndAddDataItem("Mourning");
			createAndAddDataItem("Authenticity");
			createAndAddDataItem("Creativity");
			createAndAddDataItem("Meaning");
			createAndAddDataItem("Self-worth");
			createAndAddDataItem("Acceptance");
			createAndAddDataItem("Appreciation");
			createAndAddDataItem("Closeness");
			createAndAddDataItem("Community");
			createAndAddDataItem("Consideration");
			createAndAddDataItem("Contribution to the enrichment of life");
			createAndAddDataItem("Emotional safety");
			createAndAddDataItem("Empathy");
			createAndAddDataItem("Air");
			createAndAddDataItem("Food");
			createAndAddDataItem("Movement, exercise");
			createAndAddDataItem("Protection");
			createAndAddDataItem("Rest");
			createAndAddDataItem("Sexual expression");
			createAndAddDataItem("Shelter");
			createAndAddDataItem("Touch");
			createAndAddDataItem("Water");
			createAndAddDataItem("Fun");
			createAndAddDataItem("Laughter");
			createAndAddDataItem("Beauty");
			createAndAddDataItem("Harmony");
			createAndAddDataItem("Inspiration");
			createAndAddDataItem("Order");
			createAndAddDataItem("Peace");
			createAndAddDataItem("Honesty");
			createAndAddDataItem("Love");
			createAndAddDataItem("Reassurance");
			createAndAddDataItem("Respect");
			createAndAddDataItem("Support");
			createAndAddDataItem("Trust");
			createAndAddDataItem("Understanding");
			return retTemporaryList;
			
		case KINDNESS:
			
			//Set list type and ListData
			retTemporaryList = new ListDataM(ListTypeM.KINDNESS, KindModelM.JSON_REQUESTS_KINDNESS_FILE_NAME, refContext); //Loads from JSON

			//Create and add data items
			//mKindness.loadData(ListTypeM.KINDNESS, JSON_REQUESTS_KINDNESS_FILE_NAME, mContext);
			createAndAddDataItem("Thinking about Siiri");
			createAndAddDataItem("Watching an NVC video");
			createAndAddDataItem("Expressing feelings and needs");
			createAndAddDataItem("Sleeping");
			createAndAddDataItem("Napping");
			//-To self, to another person, with or without words
			return retTemporaryList;
			
		default:
			Log.e(Utils.getClassName(), "Error in createListData: Case not covered");
			return null;
		}
	}
	
	//Please note that this is a private method that is used only in this class, and it
	// sets the isUserAddingThroughGui flag to false
	private boolean createAndAddDataItem(String inName){
		boolean retAddedSuccessfully;
		ListDataItemM tmpNewItem = new ListDataItemM(inName, refTemporaryListType); //Last value is deprecated
		//retList.addItem
		retAddedSuccessfully = retTemporaryList.addItem(tmpNewItem, false);
		return retAddedSuccessfully;
	}
}
