package es.uni_freiburg.de.cmotion;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class HeadWearableListView extends RelativeLayout implements WearableListView.OnScrollListener {


    private TextView mHeader;
    private WearableListView mList;

    public HeadWearableListView(Context context) {
        super(context);
        init();
    }

    public HeadWearableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HeadWearableListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.compound_headwearablelistview, this);

        mHeader = (TextView) getChildAt(0);
        mList = (WearableListView) getChildAt(1);

        mList.addOnScrollListener(this);
    }

    public TextView getHeader() {
        return mHeader;
    }

    public WearableListView getWearableListView() {
        return mList;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(hasFocus){
            final Animation slide = AnimationUtils.loadAnimation(getContext().getApplicationContext(), R.anim.slidein_top);
            // find the header or cache it elsewhere
            getChildAt(0).startAnimation(slide);
        }
    }


    @Override
    public void onScroll(int i) {
        mHeader.setY(mHeader.getY() - i);
    }
    @Override
    public void onAbsoluteScrollChange(int i) {

    }

    @Override
    public void onScrollStateChanged(int i) {

    }

    @Override
    public void onCentralPositionChanged(int i) {

    }
}
