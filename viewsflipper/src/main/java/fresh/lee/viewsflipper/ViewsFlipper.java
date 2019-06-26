package fresh.lee.viewsflipper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * ViewFlipper 仿淘宝、京东滚动播放控件
 *
 * @author lee
 * @date 18/7/27
 */

public class ViewsFlipper extends FrameLayout {

    private static final int DEFAULT_FLIP_DURATION = 500;

    private static final int DEFAULT_FLIP_INTERVAL = 3000;

    /**
     * animations interval
     */
    private long mFlipInterval = DEFAULT_FLIP_INTERVAL;

    /**
     * animations duration
     */
    private long mFlipDuration = DEFAULT_FLIP_DURATION;

    /**
     * 动画偏移量，涉及到View动画滚动距离，当onSizeChanged时候获取
     * （为什么不从onMeasure？是因为每次设置childView VISIBLE的时候都会触发重绘，每次都要执行onMeasure，感觉太频繁了）
     */
    private int mTranslationY;

    /**
     * view in animator
     */
    private ObjectAnimator mInAnimator;

    /**
     * view out animator
     */
    private ObjectAnimator mOutAnimator;

    /**
     * is the view visible or not.
     * default is false, changed when view visibility change. or
     * {@link ViewsFlipper#onAttachedToWindow()}
     * {@link ViewsFlipper#onDetachedFromWindow()}
     */
    private boolean mVisible = false;

    /**
     * is view begin start flipping or not
     * when call {@link ViewsFlipper#startFlipping()} set mStart true.
     * when call {@link ViewsFlipper#stopFlipping()} ()} set mStart false.
     */
    private boolean mStarted = false;

    /**
     * if flipper is running or not
     * determined by mVisible && mStarted
     */
    private boolean mRunning = false;

    /**
     * current show view index
     */
    private int mWhichChild = 0;

    /**
     * current data index, get from {@link RecyclerView.Adapter#getItemCount())
     */
    private int mPosition = 0;

    private RecyclerView.Adapter<RecyclerView.ViewHolder> mAdapter;

    /**
     * shadowed child view
     */
    private RecyclerView.ViewHolder shadowedVH;

    /**
     * showing child view
     */
    private RecyclerView.ViewHolder showingVH;

    private AnimatorSet animatorSet;

    private RecyclerView.AdapterDataObserver mObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            // when data changed, reset view.
            reset();
            animatorSet.end();

            // restart from first child view.
            mRunning = true;
            //show first child view immediately.
            mAdapter.bindViewHolder(showingVH, 0);
            setDisplayedChild(0, false);

            //cycling show next view.
            postDelayed(mFlipRunnable, mFlipInterval);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            super.onItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
            super.onItemRangeChanged(positionStart, itemCount, payload);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            super.onItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            super.onItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount);
        }
    };

    public ViewsFlipper(Context context) {
        this(context, null);
    }

    public ViewsFlipper(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    ///////////////////// public methods ///////////////////
    /////////////////////     below     ////////////////////

    /**
     * Start a timer to cycle through child views
     * without show animation.
     */
    public void startFlipping() {
        mStarted = true;
        updateRunning(false);
    }

    /**
     * stop flip
     */
    public void stopFlipping() {
        mStarted = false;
        updateRunning();
    }

    /**
     * How long to wait before flipping to the next view
     *
     * @param milliseconds times in milliseconds
     */
    public void setFlipInterval(long milliseconds) {
        this.mFlipInterval = milliseconds;
        if (mFlipInterval < mFlipDuration) {
            throw new IllegalArgumentException("flip interval must set bigger than flip duration!!!");
        }
    }

    @SuppressWarnings("unused")
    public long getFlipInterval() {
        return mFlipInterval;
    }

    /**
     * set how long flipping in/out animation cost
     *
     * @param milliseconds times in milliseconds
     */
    public void setFlipDuration(long milliseconds) {
        mFlipDuration = milliseconds;
        if (mFlipInterval < mFlipDuration) {
            throw new IllegalArgumentException("flip interval must set bigger than flip duration!!!");
        }
        mInAnimator.setDuration(milliseconds);
        mOutAnimator.setDuration(milliseconds);
    }

    @SuppressWarnings("unused")
    public long getFlipDuration() {
        return mFlipDuration;
    }

    @SuppressWarnings("unchecked cast, unused")
    public <VH extends RecyclerView.ViewHolder, T extends RecyclerView.Adapter<VH>> void setAdapter(T adapter) {
        reset();
        this.removeAllViews();
        this.mAdapter = (RecyclerView.Adapter<RecyclerView.ViewHolder>) adapter;
        this.mAdapter.registerAdapterDataObserver(mObserver);
        showingVH = mAdapter.createViewHolder(this, 0);
        shadowedVH = mAdapter.createViewHolder(this, 0);

        //add child view to parent
        addView(showingVH.itemView);
        addView(shadowedVH.itemView);
        showingVH.itemView.setVisibility(View.VISIBLE);
        shadowedVH.itemView.setVisibility(View.INVISIBLE);
        mAdapter.bindViewHolder(showingVH, 0);
    }

    private void reset() {
        removeCallbacks(mFlipRunnable);
        mPosition = 0;
        mWhichChild = 0;
        mRunning = false;
    }

    /**
     * init flipper animation and setting
     */
    private void init(Context context, AttributeSet attrs) {
        initAnimation();

        if (null != attrs) {
            /* get config from xml files */
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewsFlipper);
            setFlipDuration(a.getInteger(R.styleable.ViewsFlipper_flipDuration, DEFAULT_FLIP_DURATION));
            setFlipInterval(a.getInteger(R.styleable.ViewsFlipper_flipInterval, DEFAULT_FLIP_INTERVAL));
            a.recycle();
        }
    }

    private void initAnimation() {
        mInAnimator = defaultInAnimator();
        mOutAnimator = defaultOutAnimator();
    }

    private ObjectAnimator defaultInAnimator() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(null, "translationY", mTranslationY, 0);
        anim.setDuration(DEFAULT_FLIP_DURATION);
        return anim;
    }

    private ObjectAnimator defaultOutAnimator() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(null, "translationY", 0, -mTranslationY);
        anim.setDuration(DEFAULT_FLIP_DURATION);
        return anim;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        mTranslationY = getMeasuredHeight();
        mInAnimator.setFloatValues(mTranslationY, 0);
        mOutAnimator.setFloatValues(0, -mTranslationY);
    }

    /**
     * update view
     */
    private void updateRunning() {
        updateRunning(true);
    }

    /**
     * only show current child index view. set other view invisible.
     *
     * @param childIndex child view index
     * @param animate    is showing with animation in
     */
    void showOnly(int childIndex, boolean animate) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final View preChild = getChildAt((i == 0) ? count - 1 : i - 1);
            if (i == childIndex) {
                if (animate && mInAnimator != null) {
                    mOutAnimator.setTarget(preChild);
                    mInAnimator.setTarget(child);
                    animatorSet = new AnimatorSet();
                    animatorSet.playTogether(mOutAnimator, mInAnimator);
                    animatorSet.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);
                            child.setVisibility(View.VISIBLE);

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            preChild.setVisibility(View.INVISIBLE);
                        }

                    });
                    animatorSet.start();
                } else {
                    // if not set animation, or animate is false,
                    // then show child view immediately.
                    child.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    /**
     * begin running
     *
     * @param flipNow animation or not
     */
    private void updateRunning(boolean flipNow) {
        boolean running = mVisible && mStarted;
        if (running != mRunning) {
            if (running) {
                showOnly(mWhichChild, flipNow);
                postDelayed(mFlipRunnable, mFlipInterval);
            } else {
                removeCallbacks(mFlipRunnable);
            }
            mRunning = running;
        }
    }

    /**
     * show next view
     */
    protected void showNext() {
        // if the flipper is currently flipping automatically, and showNext() is called
        // we should we should make sure to reset the timer
        if (mRunning) {
            removeCallbacks(mFlipRunnable);
            postDelayed(mFlipRunnable, mFlipInterval);
        }

        //add child index and data index. cycling show.
        mPosition = mPosition >= mAdapter.getItemCount() - 1 ? 0 : mPosition + 1;
        mWhichChild = ((mWhichChild >= getChildCount() - 1) ? 0 : mWhichChild + 1);
        setDisplayedChild(mWhichChild);
    }

    /**
     * set display view by index in parent
     *
     * @param whichChild the display view index
     */
    private void setDisplayedChild(int whichChild) {
        setDisplayedChild(whichChild, true);
    }

    private void setDisplayedChild(int whichChild, boolean animate) {
        //swap shadowed view and showing view.
        if (showingVH.itemView.getVisibility() == View.VISIBLE) {
            swapViewHolder();
        }
        mAdapter.bindViewHolder(showingVH, mPosition);

        boolean hasFocus = getFocusedChild() != null;
        showOnly(whichChild, animate);
        if (hasFocus) {
            // Try to retake focus if we had it
            requestFocus(FOCUS_FORWARD);
        }
    }

    private void swapViewHolder() {
        RecyclerView.ViewHolder tmp = showingVH;
        showingVH = shadowedVH;
        shadowedVH = tmp;
    }

    private final Runnable mFlipRunnable = new Runnable() {
        @Override
        public void run() {
            if (mRunning) {
                ViewsFlipper.this.showNext();
            }
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mVisible = true;
        startFlipping();

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;
        stopFlipping();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mVisible = (visibility == VISIBLE);
        updateRunning(false);
    }
}