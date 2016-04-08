package cn.isif.widget.library;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ListViewPlusFooter extends LinearLayout {
	public final static int STATE_NORMAL = 0;
	public final static int STATE_READY = 1;
	public final static int STATE_LOADING = 2;
	public final static int STATE_NO_MORE = 3;
	

	private View contentView;
	private View progressBar;
	private TextView hintView;
	private int curState = STATE_NORMAL;

	public ListViewPlusFooter(Context context) {
		super(context);
		initView(context);
	}

	public ListViewPlusFooter(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context);
	}

	public void setState(int state) {
		hintView.setVisibility(View.INVISIBLE);
		progressBar.setVisibility(View.INVISIBLE);
		if (state == STATE_READY) {
			hintView.setVisibility(View.VISIBLE);
			hintView.setText(R.string.xlistview_footer_hint_ready);
		} else if (state == STATE_LOADING) {
			progressBar.setVisibility(View.VISIBLE);
		} else if (state == STATE_NO_MORE) {
			hintView.setVisibility(View.VISIBLE);
			hintView.setText(R.string.xlistview_footer_hint_no_more);		
		} else {
			hintView.setVisibility(View.VISIBLE);
			hintView.setText(R.string.xlistview_footer_hint_normal);
		}
		curState = state;
	}

	public void setBottomMargin(int height) {
		if (height < 0)
			return;
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) contentView
				.getLayoutParams();
		lp.bottomMargin = height;
		contentView.setLayoutParams(lp);
	}

	public int getBottomMargin() {
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) contentView
				.getLayoutParams();
		return lp.bottomMargin;
	}

	/**
	 * normal status
	 */
	public void normal() {
		hintView.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.GONE);
	}

	/**
	 * loading status
	 */
	public void loading() {
		hintView.setVisibility(View.GONE);
		progressBar.setVisibility(View.VISIBLE);
	}

	/**
	 * hide footer when disable pull load more
	 */
	public void hide() {
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) contentView
				.getLayoutParams();
		lp.height = 0;
		contentView.setLayoutParams(lp);
	}

	/**
	 * show footer
	 */
	public void show() {
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) contentView
				.getLayoutParams();
		lp.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
		contentView.setLayoutParams(lp);
	}

	private void initView(Context context) {
		LinearLayout moreView = (LinearLayout) LayoutInflater.from(context)
				.inflate(R.layout.pluslistview_footer, null);
		addView(moreView);
		moreView.setLayoutParams(new LinearLayout.LayoutParams(
				android.view.ViewGroup.LayoutParams.FILL_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

		contentView = moreView.findViewById(R.id.xlistview_footer_content);
		progressBar = moreView.findViewById(R.id.xlistview_footer_progressbar);
		hintView = (TextView) moreView.findViewById(R.id.xlistview_footer_hint_textview);
	}

	public void setHintTextColor(int color) {
		hintView.setTextColor(color);
	}
	
	public boolean isLoading() {
		return curState == STATE_LOADING;
	}
}
