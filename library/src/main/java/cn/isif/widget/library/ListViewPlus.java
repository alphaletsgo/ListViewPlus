/**
 * @file XListView.java
 * @package me.maxwin.view
 * @create Mar 18, 2012 6:28:41 PM
 * @author Maxwin, changed by Wang Yujian
 * @description An ListView support (a) Pull down to refresh, (b) Pull up to load more.
 * 		Implement IXListViewListener, and see stopRefresh() / stopLoadMore().
 */
package cn.isif.widget.library;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Scroller;


public class ListViewPlus extends ListView implements OnScrollListener {

	private float lastY = -1; // save event y
	private Scroller scroller; // used for scroll back
	private OnScrollListener scrollListener; // user's scroll listener

	// the interface to trigger refresh and load more.
	private Callback callback;

	// -- header view
	public ListViewPlusHeader header;
	private int headerContentHeight; // header view's height
	private boolean isEnablePullRefresh = false;

	// -- footer view
	public ListViewPlusFooter footer;
	private boolean isEnablePullLoad = false;
	private boolean isFooterReady = false;

	// total list items, used to detect is at the bottom of list view.
	private int totalItemCount;

	// for mScroller, scroll back from header or footer.
	private int scrollBack;
	private final static int SCROLLBACK_HEADER = 0;
	private final static int SCROLLBACK_FOOTER = 1;

	// scroll back duration
	private final static int SCROLL_DURATION = 400;
	// when pull up >= 50px at bottom, trigger load more.
	private final static int PULL_LOAD_MORE_DELTA = 50;
	// support iOS like pull feature.
	private final static float OFFSET_RADIO = 1.8f;

	private boolean isAutoLoadMore = true;

	private boolean canGetMore = true;
	
	private boolean mIsAutoRefreshing = false; 
	
	public ListViewPlus(Context context) {
		super(context);
		initWithContext(context);
	}

	public ListViewPlus(Context context, AttributeSet attrs) {
		super(context, attrs);
		initWithContext(context);
	}

	public ListViewPlus(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initWithContext(context);
	}

	private void initWithContext(Context context) {
		scroller = new Scroller(context, new DecelerateInterpolator());
		// XListView need the scroll event, and it will dispatch the event to
		// user's listener (as a proxy).
		super.setOnScrollListener(this);

		// init header view
		header = new ListViewPlusHeader(context);
		addHeaderView(header);

		// init footer view
		footer = new ListViewPlusFooter(context);

		// init header height
		header.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					@SuppressWarnings("deprecation")
					@Override
					public void onGlobalLayout() {
						headerContentHeight = header.getContentHeight();
						getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}
				});

		canGetMore = true;

		// initialize
		showHeader(false);
		showFooter(false);
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		// make sure XListViewFooter is the last footer view, and only add once.
		if (!isFooterReady) {
			isFooterReady = true;
			addFooterView(footer);
		}
		super.setAdapter(adapter);
	}

	/**
	 * enable or disable pull down refresh feature.
	 * 
	 * @param enable
	 */
	public void showHeader(boolean enable) {
		isEnablePullRefresh = enable;
		if (!isEnablePullRefresh) { // disable, hide the content
			header.setContentVisibility(View.INVISIBLE);
		} else {
			header.setContentVisibility(View.VISIBLE);
		}
	}

	/**
	 * enable or disable pull up load more feature.
	 * 
	 * @param enable
	 */
	public void showFooter(boolean enable) {
		isEnablePullLoad = enable;
		if (!isEnablePullLoad) {
			footer.hide();
			footer.setOnClickListener(null);
		} else {
			footer.show();
			footer.setState(ListViewPlusFooter.STATE_NORMAL);
			// both "pull up" and "click" will invoke load more.
			footer.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startLoadMore();
				}
			});
		}
	}

	/**
	 * stop refresh, reset header view.
	 */
	public void headerFinished(int state) {
		if (header.isRefreshing()) {
			header.setState(state);
			postDelayed(new Runnable() {

				@Override
				public void run() {
					resetHeaderHeight();
					if (mIsAutoRefreshing) {
						mIsAutoRefreshing = false;
					}
				}
			}, 300);
		}
	}

	private void autoRefreshing() {
		header.setState(ListViewPlusHeader.STATE_REFRESHING);
		updateHeaderHeight(160.f);
		if (callback!=null) {
			callback.onHeaderTriggerd();
		}
	}
	
	/**
	 * stop load more, reset footer view.
	 * @param state
	 */
	public void footerFinished(int state) {
		if (footer.isLoading()) {
			footer.setState(state);
		}
	}
	

	private void invokeOnScrolling() {
		if (scrollListener instanceof OnXScrollListener) {
			OnXScrollListener l = (OnXScrollListener) scrollListener;
			l.onXScrolling(this);
		}
	}

	private void updateHeaderHeight(float delta) {
		header.setVisiableHeight((int) delta + header.getVisiableHeight());
		if (isEnablePullRefresh && !header.isRefreshing()) { // 未处于刷新状态，更新箭头
			if (header.getVisiableHeight() > headerContentHeight) {
				header.setState(ListViewPlusHeader.STATE_READY);
			} else {
				header.setState(ListViewPlusHeader.STATE_NORMAL);
			}
		}
		setSelection(0); // scroll to top each time
	}

	/**
	 * reset header view's height.
	 */
	private void resetHeaderHeight() {
		int height = header.getVisiableHeight();
		if (height == 0) // not visible.
			return;
		// refreshing and header isn't shown fully. do nothing.
		if (header.isRefreshing() && height <= headerContentHeight) {
			return;
		}
		int finalHeight = 0; // default: scroll back to dismiss header.
		// is refreshing, just scroll back to show all the header.
		if (header.isRefreshing() && height > headerContentHeight) {
			finalHeight = headerContentHeight;
		}
		scrollBack = SCROLLBACK_HEADER;
		scroller.startScroll(0, height, 0, finalHeight - height,
				SCROLL_DURATION);
		// trigger computeScroll
		invalidate();
	}

	private void updateFooterHeight(float delta) {
		int height = footer.getBottomMargin() + (int) delta;
		if (isEnablePullLoad && !footer.isLoading()) {
			if (height > PULL_LOAD_MORE_DELTA) { // height enough to invoke load
													// more.
				footer.setState(ListViewPlusFooter.STATE_READY);
			} else {
				footer.setState(ListViewPlusFooter.STATE_NORMAL);
			}
		}
		footer.setBottomMargin(height);

		// setSelection(mTotalItemCount - 1); // scroll to bottom
	}

	private void resetFooterHeight() {
		int bottomMargin = footer.getBottomMargin();
		if (bottomMargin > 0) {
			scrollBack = SCROLLBACK_FOOTER;
			scroller.startScroll(0, bottomMargin, 0, -bottomMargin,
					SCROLL_DURATION);
			invalidate();
		}
	}

	public void setAutoRefreshing() {
		mIsAutoRefreshing = true;
		postDelayed(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				autoRefreshing();
			}
		}, 300);
	}
	
	private void startLoadMore() {
		if (!footer.isLoading()) {
			footer.setState(ListViewPlusFooter.STATE_LOADING);
			if (callback != null) {
				callback.onFooterTriggerd();
			}
		}
	}

	private float lastDownY;
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (lastY == -1) {
			lastY = ev.getRawY();
		}
		
		if(lastDownY == -1){
			lastDownY = ev.getRawY();
		}
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			lastY = ev.getRawY();
			lastDownY = ev.getRawY();
			break;
		case MotionEvent.ACTION_MOVE:
			final float deltaY = ev.getRawY() - lastY;
			lastY = ev.getRawY();
			if (getFirstVisiblePosition() == 0
					&& (header.getVisiableHeight() > 0 || deltaY > 0)) {
				// the first item is showing, header has shown or pull down.
				invokeOnScrolling();
				updateHeaderHeight(deltaY / OFFSET_RADIO);
			} else if (getLastVisiblePosition() == totalItemCount - 1
					&& (footer.getBottomMargin() > 0 || deltaY < 0)) {
				// last item, already pulled up or want to pull up.
				updateFooterHeight(-deltaY / OFFSET_RADIO);
			}
			break;
		case MotionEvent.ACTION_UP:
			lastY = -1; // reset
			lastDownY = -1;
			if (getFirstVisiblePosition() == 0) {
				// invoke refresh
				if (isEnablePullRefresh
						&& header.getVisiableHeight() > headerContentHeight) {
					header.setState(ListViewPlusHeader.STATE_REFRESHING);
					if (mIsAutoRefreshing) {
						resetHeaderHeight();
						return super.onTouchEvent(ev);
					}
					if (callback != null) {
						callback.onHeaderTriggerd();
					}
				}
				resetHeaderHeight();
			} else if (getLastVisiblePosition() == totalItemCount - 1) {
				// invoke load more.
				if (isEnablePullLoad) {
					if (isAutoLoadMore) {
						startLoadMore();
					} else if (footer.getBottomMargin() > PULL_LOAD_MORE_DELTA) {
						startLoadMore();
					}
				}
				resetFooterHeight();
			}
			
			break;
		case MotionEvent.ACTION_CANCEL:
			break;
		}
		return super.onTouchEvent(ev);
	}

	@Override
	public void computeScroll() {
		if (scroller.computeScrollOffset()) {
			if (scrollBack == SCROLLBACK_HEADER) {
				header.setVisiableHeight(scroller.getCurrY());
			} else {
				footer.setBottomMargin(scroller.getCurrY());
			}
			postInvalidate();
			invokeOnScrolling();
		}
		super.computeScroll();
	}

	@Override
	public void setOnScrollListener(OnScrollListener l) {
		scrollListener = l;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollListener != null) {
			scrollListener.onScrollStateChanged(view, scrollState);
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// send to user's listener
		this.totalItemCount = totalItemCount;
		if (scrollListener != null) {
			scrollListener.onScroll(view, firstVisibleItem, visibleItemCount,
					totalItemCount);
		}

		if (canGetMore) {
			if (firstVisibleItem + visibleItemCount == totalItemCount) {
				// footer is on screen
				// invoke load more.
				if (isEnablePullLoad) {
					if (isAutoLoadMore) {
						canGetMore = false;
						startLoadMore();
					} else if (footer.getBottomMargin() > PULL_LOAD_MORE_DELTA) {
						startLoadMore();
					}
				}
			}
		} else {
			if (firstVisibleItem + visibleItemCount < totalItemCount) {
				canGetMore = true;
			}
		}

	}

	public void setCallback(Callback c) {
		callback = c;
	}

	/**
	 * you can listen ListView.OnScrollListener or this one. it will invoke
	 * onXScrolling when header/footer scroll back.
	 */
	public interface OnXScrollListener extends OnScrollListener {
		public void onXScrolling(View view);
	}

	/**
	 * implements this interface to get refresh/load more event.
	 */
	public interface Callback {

		public void onHeaderTriggerd();

		public void onFooterTriggerd();
	}

	public void setHintTextColor(int color) {
		if (header != null) {
			header.setHintTextColor(color);
		}

		if (footer != null) {
			footer.setHintTextColor(color);
		}
	}

	public void setTimeTextColor(int color) {
		if (header != null) {
			header.setTimeTextColor(color);
		}
	}

	public void setIsAutoLoadMore(boolean isAutoLoadMore) {
		this.isAutoLoadMore = isAutoLoadMore;
	}
	
//	@Override
//	/**
//	 * 重写该方法，达到使ListView适应ScrollView的效果
//	 */
//	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//		int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
//		super.onMeasure(widthMeasureSpec, expandSpec);
//	}
}
