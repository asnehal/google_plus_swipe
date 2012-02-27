/*
 * Copyright (C) 2011 Jake Wharton
 * Copyright (C) 2011 Patrik Akerfeldt
 * Copyright (C) 2011 Francisco Figueiredo Jr.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.swipe.viewpager;

import java.util.ArrayList;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.View;

/**
 * A TitlePageIndicator is a PageIndicator which displays the title of left view
 * (if exist), the title of the current selected view (centered) and the title of
 * the right view (if exist). When the user scrolls the ViewPager then titles are
 * also scrolled.
 */
public class TitlePageIndicator extends View implements PageIndicator {
    /**
     * Percentage indicating what percentage of the screen width away from
     * center should the underline be fully faded. A value of 0.25 means that
     * halfway between the center of the screen and an edge.
     */
    private static final float SELECTION_FADE_PERCENTAGE = 0.25f;

    /**
     * Percentage indicating what percentage of the screen width away from
     * center should the selected text bold turn off. A value of 0.05 means
     * that 10% between the center and an edge.
     */
    private static final float BOLD_FADE_PERCENTAGE = 0.05f;

    /**
     * Interface for a callback when the center item has been clicked.
     */
    public static interface OnCenterItemClickListener {
        /**
         * Callback when the center item has been clicked.
         *
         * @param position Position of the current center item.
         */
        public void onCenterItemClick(int position);
    }

    public enum IndicatorStyle {
        None(0), Triangle(1), Underline(2);

        public final int value;

        private IndicatorStyle(int value) {
            this.value = value;
        }

        public static IndicatorStyle fromValue(int value) {
            for (IndicatorStyle style : IndicatorStyle.values()) {
                if (style.value == value) {
                    return style;
                }
            }
            return null;
        }
    }

    private ViewPager mViewPager;
    private TitleProvider mTitleProvider;
    private int mCurrentPage;
    private int mCurrentOffset;
    private int mScrollState;
    private final Paint mPaintText = new Paint();
    private boolean mBoldText;
    private int mColorText;
    private int mColorSelected;
    private Path mPath;
    private final Paint mPaintFooterLine = new Paint();
    private IndicatorStyle mFooterIndicatorStyle;
    private final Paint mPaintFooterIndicator = new Paint();
    private float mFooterIndicatorHeight;
    private float mFooterIndicatorUnderlinePadding;
    private float mFooterPadding;
    private float mTitlePadding;
    private float mTopPadding;
    /** Left and right side padding for not active view titles. */
    private float mClipPadding;
    private float mFooterLineHeight;
    
    private ArrayList<RectF> list = new ArrayList<RectF>();

    public TitlePageIndicator(Context context) {
    	 this(context, null);       
    }

    public TitlePageIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.vpiTitlePageIndicatorStyle);
    }

    public TitlePageIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        //Load defaults from resources
        final Resources res = getResources();
        final int defaultFooterColor = res.getColor(R.color.default_title_indicator_footer_color);
        final float defaultFooterLineHeight = res.getDimension(R.dimen.default_title_indicator_footer_line_height);
        final int defaultFooterIndicatorStyle = res.getInteger(R.integer.default_title_indicator_footer_indicator_style);
        final float defaultFooterIndicatorHeight = res.getDimension(R.dimen.default_title_indicator_footer_indicator_height);
        final float defaultFooterIndicatorUnderlinePadding = res.getDimension(R.dimen.default_title_indicator_footer_indicator_underline_padding);
        final float defaultFooterPadding = res.getDimension(R.dimen.default_title_indicator_footer_padding);
        final int defaultSelectedColor = res.getColor(R.color.default_title_indicator_selected_color);
        final boolean defaultSelectedBold = res.getBoolean(R.bool.default_title_indicator_selected_bold);
        final int defaultTextColor = res.getColor(R.color.default_title_indicator_text_color);
        final float defaultTextSize = res.getDimension(R.dimen.default_title_indicator_text_size);
        final float defaultTitlePadding = res.getDimension(R.dimen.default_title_indicator_title_padding);
        final float defaultClipPadding = res.getDimension(R.dimen.default_title_indicator_clip_padding);
        final float defaultTopPadding = res.getDimension(R.dimen.default_title_indicator_top_padding);

        //Retrieve styles attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TitlePageIndicator, defStyle, R.style.Widget_TitlePageIndicator);

        //Retrieve the colors to be used for this view and apply them.
        mFooterLineHeight = a.getDimension(R.styleable.TitlePageIndicator_footerLineHeight, defaultFooterLineHeight);
        mFooterIndicatorStyle = IndicatorStyle.fromValue(a.getInteger(R.styleable.TitlePageIndicator_footerIndicatorStyle, defaultFooterIndicatorStyle));
        mFooterIndicatorHeight = a.getDimension(R.styleable.TitlePageIndicator_footerIndicatorHeight, defaultFooterIndicatorHeight);
        mFooterIndicatorUnderlinePadding = a.getDimension(R.styleable.TitlePageIndicator_footerIndicatorUnderlinePadding, defaultFooterIndicatorUnderlinePadding);
        mFooterPadding = a.getDimension(R.styleable.TitlePageIndicator_footerPadding, defaultFooterPadding);
        mTopPadding = a.getDimension(R.styleable.TitlePageIndicator_topPadding, defaultTopPadding);
        mTitlePadding = a.getDimension(R.styleable.TitlePageIndicator_titlePadding, defaultTitlePadding);
        mClipPadding = a.getDimension(R.styleable.TitlePageIndicator_clipPadding, defaultClipPadding);
        mColorSelected = a.getColor(R.styleable.TitlePageIndicator_selectedColor, defaultSelectedColor);
        mColorText = a.getColor(R.styleable.TitlePageIndicator_textColor, defaultTextColor);
        mBoldText = a.getBoolean(R.styleable.TitlePageIndicator_selectedBold, defaultSelectedBold);

        final float textSize = a.getDimension(R.styleable.TitlePageIndicator_textSize, defaultTextSize);
        final int footerColor = a.getColor(R.styleable.TitlePageIndicator_footerColor, defaultFooterColor);
        mPaintText.setTextSize(textSize);
        mPaintText.setAntiAlias(true);
        mPaintFooterLine.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaintFooterLine.setStrokeWidth(mFooterLineHeight);
        mPaintFooterLine.setColor(footerColor);
        mPaintFooterIndicator.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaintFooterIndicator.setColor(footerColor);

        a.recycle();
    }

    public int getFooterColor() {
        return mPaintFooterLine.getColor();
    }

    public void setFooterColor(int footerColor) {
        mPaintFooterLine.setColor(footerColor);
        mPaintFooterIndicator.setColor(footerColor);
        invalidate();
    }

    public float getFooterLineHeight() {
        return mFooterLineHeight;
    }

    public void setFooterLineHeight(float footerLineHeight) {
        mFooterLineHeight = footerLineHeight;
        mPaintFooterLine.setStrokeWidth(mFooterLineHeight);
        invalidate();
    }

    public float getFooterIndicatorHeight() {
        return mFooterIndicatorHeight;
    }

    public void setFooterIndicatorHeight(float footerTriangleHeight) {
        mFooterIndicatorHeight = footerTriangleHeight;
        invalidate();
    }

    public float getFooterIndicatorPadding() {
        return mFooterPadding;
    }

    public void setFooterIndicatorPadding(float footerIndicatorPadding) {
        mFooterPadding = footerIndicatorPadding;
        invalidate();
    }

    public IndicatorStyle getFooterIndicatorStyle() {
        return mFooterIndicatorStyle;
    }

    public void setFooterIndicatorStyle(IndicatorStyle indicatorStyle) {
        mFooterIndicatorStyle = indicatorStyle;
        invalidate();
    }

    public int getSelectedColor() {
        return mColorSelected;
    }

    public void setSelectedColor(int selectedColor) {
        mColorSelected = selectedColor;
        invalidate();
    }

    public boolean isSelectedBold() {
        return mBoldText;
    }

    public void setSelectedBold(boolean selectedBold) {
        mBoldText = selectedBold;
        invalidate();
    }

    public int getTextColor() {
        return mColorText;
    }

    public void setTextColor(int textColor) {
        mPaintText.setColor(textColor);
        mColorText = textColor;
        invalidate();
    }

    public float getTextSize() {
        return mPaintText.getTextSize();
    }

    public void setTextSize(float textSize) {
        mPaintText.setTextSize(textSize);
        invalidate();
    }

    public float getTitlePadding() {
        return this.mTitlePadding;
    }

    public void setTitlePadding(float titlePadding) {
        mTitlePadding = titlePadding;
        invalidate();
    }

    public float getTopPadding() {
        return this.mTopPadding;
    }

    public void setTopPadding(float topPadding) {
        mTopPadding = topPadding;
        invalidate();
    }

    public float getClipPadding() {
        return this.mClipPadding;
    }

    public void setClipPadding(float clipPadding) {
        mClipPadding = clipPadding;
        invalidate();
    }

    public void setTypeface(Typeface typeface) {
        mPaintText.setTypeface(typeface);
        invalidate();
    }

    public Typeface getTypeface() {
        return mPaintText.getTypeface();
    }

    /*
     *This function is called when the view should render its contents.
     */
    @Override
    protected void onDraw(Canvas canvas) {
    	
        super.onDraw(canvas);

        if (mViewPager == null) {
            return;
        }
        
        final int count = mViewPager.getAdapter().getCount();
        if (count == 0) {
            return;
        }
        
        //Calculate bounds of titles
        ArrayList<RectF> bounds = calculateAllBounds(mPaintText);
        final int boundsSize = bounds.size();


        final float halfWidth = getWidth() / 2f;
        final int left = getLeft();        
        final int width = getWidth();
       
        final int height = getHeight();
        final int right = left + width;

        int page = mCurrentPage;
        float offsetPercent;
        if (mCurrentOffset <= halfWidth) {
            offsetPercent = 1.0f * mCurrentOffset / width;
        } else {
            page += 1;
            offsetPercent = 1.0f * (width - mCurrentOffset) / width;
        }
        final boolean currentSelected = (offsetPercent <= SELECTION_FADE_PERCENTAGE);
        final boolean currentBold = (offsetPercent <= BOLD_FADE_PERCENTAGE);
        final float selectedPercent = (SELECTION_FADE_PERCENTAGE - offsetPercent) / SELECTION_FADE_PERCENTAGE;

        //Left views starting from the current position
        /*leftDiff: distance between left bound of previous page(left) title and left bound of current page title
         * rightDiff: distance between left bound of next page(right) page title and left bound of current page title
         * leftDiff and rightDiff will be constant i.e.this distance should be maintained throughout
         */
        RectF current = bounds.get(mCurrentPage); //get current title position specs
    
    	float leftDiff = halfWidth - ((current.right - current.left)/2);
        float rightDiff = width - (leftDiff) - (current.right - current.left);
            
        int leftCounter = 1;
        int rightCounter = 1;
        //Fix positions of titles other than current page title
        //Left views starting from the current position
        for (int i = mCurrentPage - 1; i >= 0; i--) {
        	RectF bound = bounds.get(i); //get previous title position specs
        	float w = bound.right - bound.left;
            RectF rightBound = bounds.get(mCurrentPage); //get current title position specs
            bound.left = rightBound.left - (leftCounter *leftDiff); 
            bound.right = bound.left + w;
            leftCounter++;
        }
    
        //Right views starting from the current position
        for (int i = mCurrentPage + 1 ; i < count; i++) {
            RectF bound = bounds.get(i);
            float w = bound.right - bound.left;
            RectF leftBound = bounds.get(mCurrentPage);
            bound.left = leftBound.left + (rightCounter *rightDiff);
            bound.right = bound.left + w;
            rightCounter++;
        }

        //Now draw views
        //original
        int colorTextAlpha = mColorText >>> 24;
        for (int i = 0; i < count; i++) {
            //Get the title
            RectF bound = bounds.get(i);
            //Only if one side is visible
            if ((bound.left > left && bound.left < right) || (bound.right > left && bound.right < right)) {
                final boolean currentPage = (i == page);
            	
                //Only set bold if we are within bounds
                mPaintText.setFakeBoldText(currentPage && currentBold && mBoldText);

                //Draw text as unselected
                mPaintText.setColor(mColorText);
                if(currentPage && currentSelected) {
                    //Fade out/in unselected text as the selected text fades in/out
                    mPaintText.setAlpha(colorTextAlpha - (int)(colorTextAlpha * selectedPercent));
                }
            
                canvas.drawText(mTitleProvider.getTitle(i), bound.left, bound.bottom + mTopPadding, mPaintText);
                //If we are within the selected bounds draw the selected text
                if (currentPage && currentSelected) {
                    mPaintText.setColor(mColorSelected);
                    mPaintText.setAlpha((int)((mColorSelected >>> 24) * selectedPercent));
                    canvas.drawText(mTitleProvider.getTitle(i), bound.left, bound.bottom + mTopPadding, mPaintText);
                }
            }
        }

        //Draw the footer line
        mPath = new Path();
        mPath.moveTo(0, height - mFooterLineHeight / 2f);
        mPath.lineTo(width, height - mFooterLineHeight / 2f);
        mPath.close();
        canvas.drawPath(mPath, mPaintFooterLine);
        
        switch (mFooterIndicatorStyle) {
            case Triangle:
                mPath = new Path();
                mPath.moveTo(halfWidth, height - mFooterLineHeight - mFooterIndicatorHeight);
                mPath.lineTo(halfWidth + mFooterIndicatorHeight, height - mFooterLineHeight);
                mPath.lineTo(halfWidth - mFooterIndicatorHeight, height - mFooterLineHeight);
                mPath.close();
                canvas.drawPath(mPath, mPaintFooterIndicator);
                break;

            case Underline:
                if (!currentSelected || page >= boundsSize) {
                    break;
                }
                RectF underlineBounds = bounds.get(page);
                mPath = new Path();
                mPath.moveTo(underlineBounds.left  - mFooterIndicatorUnderlinePadding, height - mFooterLineHeight);
                mPath.lineTo(underlineBounds.right + mFooterIndicatorUnderlinePadding, height - mFooterLineHeight);
                mPath.lineTo(underlineBounds.right + mFooterIndicatorUnderlinePadding, height - mFooterLineHeight - mFooterIndicatorHeight);
                mPath.lineTo(underlineBounds.left  - mFooterIndicatorUnderlinePadding, height - mFooterLineHeight - mFooterIndicatorHeight);
                mPath.close();

                mPaintFooterIndicator.setAlpha((int)(0xFF * selectedPercent));
                canvas.drawPath(mPath, mPaintFooterIndicator);
                mPaintFooterIndicator.setAlpha(0xFF);
                break;
        }
    }

    /**
     * Calculate views bounds and scroll them according to the current index
     *
     * @param paint
     * @param currentIndex
     * @return
     */
    private ArrayList<RectF> calculateAllBounds(Paint paint) {
    	list.clear();
        //For each views (If no values then add a fake one)
        final int count = mViewPager.getAdapter().getCount();
        final int width = getWidth();
        final int halfWidth = width /2;
        for (int i = 0; i < count; i++) {
            RectF bounds = calcBounds(i, paint);
            float w = (bounds.right - bounds.left);
            float h = (bounds.bottom - bounds.top);
            
            bounds.left = (float) ((halfWidth) - (w / 2) - (mCurrentOffset - (mCurrentOffset *0.58)) + ((i - mCurrentPage) * width)) ;	//original formula 
            bounds.right = bounds.left + w;
            bounds.top = 0;
            bounds.bottom = h;
            list.add(bounds);
        }
        return list;
    }

    /**
     * Calculate the bounds for a view's title
     *
     * @param index
     * @param paint
     * @return
     */
    private RectF calcBounds(int index, Paint paint) {
        //Calculate the text bounds
        RectF bounds = new RectF();
        bounds.right = paint.measureText(mTitleProvider.getTitle(index));	//width of the text
        bounds.bottom = paint.descent() - paint.ascent();
        return bounds;
    }

    @Override
    public void setViewPager(ViewPager view) {
        final PagerAdapter adapter = view.getAdapter();
        if (adapter == null) {
            throw new IllegalStateException("ViewPager does not have adapter instance.");
        }
        if (!(adapter instanceof TitleProvider)) {
            throw new IllegalStateException("ViewPager adapter must implement TitleProvider to be used with TitlePageIndicator.");
        }
        mViewPager = view;

        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int position) {
				// TODO Auto-generated method stub
		        if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
		            mCurrentPage = position;
		            invalidate();
		        }
			}
			
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				// TODO Auto-generated method stub
				mCurrentPage = position;
		        mCurrentOffset = positionOffsetPixels;
		        invalidate();
			}
			
			@Override
			public void onPageScrollStateChanged(int state) {
				// TODO Auto-generated method stub
				mScrollState = state;
			}
		});
    
        mTitleProvider = (TitleProvider)adapter;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Measure our width in whatever mode specified
        final int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);

        //Determine our height
        float height = 0;
        final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightSpecMode == MeasureSpec.EXACTLY) {
            //We were told how big to be
            height = MeasureSpec.getSize(heightMeasureSpec);
        } else {
            //Calculate the text bounds
            RectF bounds = new RectF();
            bounds.bottom = mPaintText.descent()-mPaintText.ascent();
            height = bounds.bottom - bounds.top + mFooterLineHeight + mFooterPadding + mTopPadding;
            if (mFooterIndicatorStyle != IndicatorStyle.None) {
                height += mFooterIndicatorHeight;
            }
        }
        final int measuredHeight = (int)height;

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState)state;
        super.onRestoreInstanceState(savedState.getSuperState());
        mCurrentPage = savedState.currentPage;
        requestLayout();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.currentPage = mCurrentPage;
        return savedState;
    }

    static class SavedState extends BaseSavedState {
        int currentPage;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            currentPage = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(currentPage);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageSelected(int arg0) {
		// TODO Auto-generated method stub
		
	}
}
