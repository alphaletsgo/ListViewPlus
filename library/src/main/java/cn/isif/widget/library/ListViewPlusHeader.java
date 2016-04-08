package cn.isif.widget.library;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ListViewPlusHeader extends LinearLayout {

    private LinearLayout container;
    private ImageView arrowImageView;
    private ProgressBar progressBar;
    private TextView hintView;
    private int curState = STATE_NORMAL;

    private Animation rotateUpAnim;
    private Animation rotateDownAnim;

    private final int ROTATE_ANIM_DURATION = 180;

    public final static int STATE_NORMAL = 0;
    public final static int STATE_READY = 1;
    public final static int STATE_REFRESHING = 2;
    public final static int STATE_SUCCESS = 3;
    public final static int STATE_FAIL = 4;
    public final static int STATE_SEARCH_FAIL = 5;

    // header view content, use it to calculate the Header's height. And hide it
    // when disable pull refresh.
    private RelativeLayout contentView;
    private TextView timeView;

    private static final int DEFAULT_HINT_COLOR = 0xFF333333;
    private static final int DEFAULT_TIME_COLOR = 0xFF666666;

    @SuppressLint("SimpleDateFormat")
    static private SimpleDateFormat sSDF = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm");
    private Date date = null;
    private long lastUpdateTime = 0L;

    public ListViewPlusHeader(Context context) {
        super(context);
        initView(context);
    }

    public ListViewPlusHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        date = new Date();
        //
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0);
        container = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.pluslistview_header, null);
        addView(container, lp);
        setGravity(Gravity.BOTTOM);
        contentView = (RelativeLayout) findViewById(R.id.xlistview_header_content);
        arrowImageView = (ImageView) findViewById(R.id.xlistview_header_arrow);
        hintView = (TextView) findViewById(R.id.xlistview_header_hint_textview);
        hintView.setTextColor(DEFAULT_HINT_COLOR);
        timeView = (TextView) findViewById(R.id.xlistview_header_time);
        timeView.setTextColor(DEFAULT_TIME_COLOR);
        progressBar = (ProgressBar) findViewById(R.id.xlistview_header_progressbar);
        rotateUpAnim = new RotateAnimation(0.0f, -180.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateUpAnim.setDuration(ROTATE_ANIM_DURATION);
        rotateUpAnim.setFillAfter(true);
        rotateDownAnim = new RotateAnimation(-180.0f, 0.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateDownAnim.setDuration(ROTATE_ANIM_DURATION);
        rotateDownAnim.setFillAfter(true);

    }

    public void setState(int state) {
        if (state == curState)
            return;

        if (state == STATE_REFRESHING) {
            arrowImageView.clearAnimation();
            arrowImageView.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        } else if (state == STATE_SUCCESS || state == STATE_FAIL) {
            arrowImageView.clearAnimation();
            arrowImageView.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
        } else {
            arrowImageView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
        }

        switch (state) {
            case STATE_NORMAL:
                if (curState == STATE_READY) {
                    arrowImageView.startAnimation(rotateDownAnim);
                }
                if (curState == STATE_REFRESHING) {
                    arrowImageView.clearAnimation();
                }
                hintView.setText(R.string.xlistview_header_hint_normal);

                setTimeView(lastUpdateTime);
                break;
            case STATE_READY:
                if (curState != STATE_READY) {
                    arrowImageView.clearAnimation();
                    arrowImageView.startAnimation(rotateUpAnim);
                    hintView.setText(R.string.xlistview_header_hint_ready);
                }
                setTimeView(lastUpdateTime);
                break;
            case STATE_REFRESHING:
                hintView.setText(R.string.xlistview_header_hint_loading);
                setTimeView(lastUpdateTime);
                break;
            case STATE_SUCCESS:
                hintView.setText(R.string.xlistview_header_hint_success);
                lastUpdateTime = System.currentTimeMillis();
                setTimeView(lastUpdateTime);
                break;
            case STATE_FAIL:
                hintView.setText(R.string.xlistview_header_hint_fail);
                timeView.setVisibility(View.GONE);
                break;
            case STATE_SEARCH_FAIL:
                hintView.setText("无搜索结果");
                timeView.setVisibility(View.GONE);
                break;

        }
        curState = state;
    }

    public void setVisiableHeight(int height) {
        if (height < 0)
            height = 0;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) container.getLayoutParams();
        lp.height = height;
        container.setLayoutParams(lp);
    }

    public int getVisiableHeight() {
        return container.getHeight();
    }

    public int getContentHeight() {
        return contentView.getHeight();
    }

    public void setContentVisibility(int visibility) {
        contentView.setVisibility(visibility);
    }

    public boolean isRefreshing() {
        return curState == STATE_REFRESHING;
    }

    private void setTimeView(long lastUpdate) {
        if (lastUpdate > 0L) {
            date.setTime(lastUpdate);
            String time = getResources().getString(R.string.xlistview_header_last_time, sSDF.format(date));
            timeView.setVisibility(View.VISIBLE);
            timeView.setText(time);
        } else {
            timeView.setVisibility(View.GONE);
        }
    }

    public void setHintTextColor(int color) {
        hintView.setTextColor(color);
    }

    public void setTimeTextColor(int color) {
        timeView.setTextColor(color);
    }
}
