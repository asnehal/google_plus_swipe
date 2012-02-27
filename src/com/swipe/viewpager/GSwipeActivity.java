package com.swipe.viewpager;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

public class GSwipeActivity extends Activity {
    /** Called when the activity is first created. */
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
	    super.onCreate( savedInstanceState );
	    setContentView( R.layout.main );
	 
	    ViewPagerAdapter adapter = new ViewPagerAdapter( this );
	    ViewPager pager =(ViewPager)findViewById( R.id.viewpager );
	    TitlePageIndicator indicator = (TitlePageIndicator)findViewById( R.id.indicator );
	    pager.setAdapter( adapter );
	    pager.setCurrentItem(0);
	    indicator.setViewPager( pager );
	}
}