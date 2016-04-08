package cn.isif.widget.library.pulltorefresh;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created by Administrator on 2016/3/18 0018.
 */
public class MyScrollView extends ScrollView{
    private ScrollChangeListener myListener;

    public MyScrollView(Context context) {
        super(context);
    }

    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if(myListener != null){
            myListener.onSchange(l, t, oldl, oldt);
        }
    }
    public void setScrollChangeListener(ScrollChangeListener listener){
        myListener = listener;
    }

    public interface ScrollChangeListener{
        void onSchange(int l, int t, int oldl, int oldt);
    }

}
