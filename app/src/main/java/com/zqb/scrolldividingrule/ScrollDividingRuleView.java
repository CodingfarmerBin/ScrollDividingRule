package com.zqb.scrolldividingrule;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Scroller;
import java.util.ArrayList;

/**
 * 横向滚动的刻度尺
 * Created by zhangqingbin on 2018/1/5.
 */

public class ScrollDividingRuleView extends View {

  private Scroller mScroller;
  private Paint mPaint;
  private VelocityTracker mVelocityTracker;
  private OnScrollListener mListener;
  private int mScaleMargin; //刻度间距
  private float mScaleWidth; //总刻度宽度
  private float mTextSize;//文字大小
  private ArrayList<String> mTextList;//刻度文字
  private ArrayList<String> mTotalTextList;//所有的刻度
  private float mLineHeight;//线高度
  private int mRectHeight;//总高度
  private int mScrollLastX;
  private int mInitDistance;//初始距离
  private int mInitPosition;//初始位置
  private int mTextLineMargin;//文字距线的距离
  private int mPage;//当前为复用所在页
  private long mLastInstance;//余数刻度
  private long mStartMoney;//开始金额
  private int mUnit;//每个小格的单位
  private long mFinalMoney;//结束金额

  public ScrollDividingRuleView(Context context) {
    this(context, null);
  }

  public ScrollDividingRuleView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ScrollDividingRuleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setWillNotDraw(false);
    init(context, attrs);
  }

  private void init(Context context, @Nullable AttributeSet attrs) {
    if (attrs != null) {
      for (int i = 0; i < attrs.getAttributeCount(); i++) {
        String name = attrs.getAttributeName(i);
        if ("layout_width".equals(name)) {
          String value = attrs.getAttributeValue(i);
          if (value.length() > 2) {
            if (value.endsWith("dp")) {
              float margin = Float.valueOf(value.substring(0, value.length() - 2));
              mScaleWidth = Utils.dp2px(context, margin);
            } else {
              mScaleWidth = Float.valueOf(value.substring(0, value.length() - 2));
            }
          } else if (value.equals("-1") || value.equals("-2") || value.equals("0")) {
            mScaleWidth = 0;
          }
        } else if ("line_height".equals(name)) {
          String value = attrs.getAttributeValue(i);
          if (value.length() > 2) {
            if (value.endsWith("dp")) {
              mLineHeight =
                  Utils.dp2px(context, Float.valueOf(value.substring(0, value.length() - 2)));
            } else {
              mLineHeight = Float.valueOf(value.substring(0, value.length() - 2));
            }
          } else {
            mLineHeight = 50;
          }
        } else if ("dividing_text_size".equals(name)) {
          String value = attrs.getAttributeValue(i);
          if (value.length() > 2) {
            if (value.endsWith("sp")) {
              mTextSize =
                  Utils.sp2px(context, Float.valueOf(value.substring(0, value.length() - 2)));
            } else {
              mTextSize = Float.valueOf(value.substring(0, value.length() - 2));
            }
          } else {
            mTextSize = 32;
          }
        }
      }
    }
    // 画笔
    mPaint = new Paint();
    //总的高度，因为text的高度和设置的textSize会有误差所以加上20的高度
    mRectHeight = (int) (mLineHeight + mTextSize + mTextLineMargin + 20);
    //初始设置每个刻度间距为30px
    mScaleMargin = 20;
    mTextList = new ArrayList<>();
    mTotalTextList = new ArrayList<>();
    mScroller = new Scroller(context);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    mPaint.setColor(Color.GRAY);
    // 抗锯齿
    mPaint.setAntiAlias(true);
    // 设定是否使用图像抖动处理，会使绘制出来的图片颜色更加平滑和饱满，图像更加清晰
    mPaint.setDither(true);
    // 空心
    mPaint.setStyle(Paint.Style.STROKE);
    // 文字居中
    mPaint.setTextAlign(Paint.Align.CENTER);
    onDrawScale(canvas, mPaint); //画刻度
    onDrawLine(canvas, mPaint);//画刻度中间横线
    super.onDraw(canvas);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int height = MeasureSpec.makeMeasureSpec(mRectHeight, MeasureSpec.AT_MOST);
    super.onMeasure(widthMeasureSpec, height);
    //初始化开始位置
    mInitDistance = getMeasuredWidth() / 2 - mInitPosition * mScaleMargin * 10;
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (mVelocityTracker == null) {
      mVelocityTracker = VelocityTracker.obtain();
    }
    mVelocityTracker.addMovement(event);
    int x = (int) event.getX();
    int startX = 0;
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        if (mScroller != null) {//重新初始化fling效果，防止下一次移动有初始速度
          mScroller.fling(mScroller.getFinalX(), mScroller.getFinalY(), 0, 0, 0,
              (int) (mScaleWidth - mInitPosition * mScaleMargin * 10), 0, 0);
          mScroller.abortAnimation();
        }
        startX = x;
        mScrollLastX = x;
        return true;
      case MotionEvent.ACTION_MOVE:
        int dataX = mScrollLastX - x;
        smoothScrollBy(dataX, 0);
        mScrollLastX = x;
        return true;
      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        dealActionUp(x - startX);
        return true;
    }
    return super.onTouchEvent(event);
  }

  /**
   * 处理手势抬起之后的操作
   *
   * @param i 根据正负判断方向
   */
  private void dealActionUp(int i) {
    mVelocityTracker.computeCurrentVelocity(1000);
    if (mScroller.getFinalX() >= mScaleMargin * 10 * 10 * 2 - 10 * mScaleMargin && i > 0) {
      if (!notifyDataChanged(0)) {
        dealFling();
      }
    } else if (mScroller.getFinalX() <= mScaleMargin * 10 * 10 - 10 * mScaleMargin && i < 0) {
      if (!notifyDataChanged(1)) {
        dealFling();
      }
    } else {
      dealFling();
    }
    long i1 = mPage > 0 ? mPage * 100 : 0;
    if (mListener != null) {
      mListener.onScaleScrollChanged(
          mScroller.getFinalX() / mScaleMargin + mInitPosition * 10 + i1 + mStartMoney);//返回滚动选中的位置
    }
    mVelocityTracker.clear();
    mVelocityTracker.recycle();
    mVelocityTracker = null;
  }

  /**
   * 处理手指抬起之后的减速滚动
   */
  private void dealFling() {
    int minX;//最小值
    if (mPage == 0) {
      minX = -mInitPosition * mScaleMargin * 10;
    } else {
      minX = -mInitPosition * mScaleMargin * 10 + mScaleMargin * 10 * 10 - mScaleMargin * 10;
    }
    int maxX;//最大值 根据位置和总数改变
    if (mPage == (mTotalTextList.size() - 20) / 10 - 1 || mTotalTextList.size() < 40) {
      maxX = (int) (mScaleWidth - mInitPosition * mScaleMargin * 10);
    } else {
      maxX = mScaleMargin * 10 * 20 - mScaleMargin * 10;
    }
    mScroller.fling(mScroller.getFinalX(), mScroller.getFinalY(),
        -(int) mVelocityTracker.getXVelocity(), 0, minX, maxX, 0, 0);
  }

  private void onDrawScale(Canvas canvas, Paint paint) {
    paint.setAntiAlias(true);
    paint.setTextSize(mTextSize);
    paint.setStyle(Paint.Style.FILL);
    for (int i = 0, k = 0; i < mTextList.size() * 10; i++) {
      if (i < mTextList.size() * 10 - 9) {
        if (i % 10 == 0) { //整值
          paint.setColor(Color.GRAY);
          canvas.drawLine(i * mScaleMargin + mInitDistance, mRectHeight,
              i * mScaleMargin + mInitDistance, mRectHeight - mLineHeight - mTextLineMargin + 20,
              paint);
          //整值文字
          paint.setColor(Color.GRAY);
          canvas.drawText(mTextList.get(k), i * mScaleMargin + mInitDistance,
              mRectHeight - mLineHeight - mTextLineMargin, paint);
          k++;
        } else {
          paint.setColor(Color.GRAY);
          canvas.drawLine(i * mScaleMargin + mInitDistance, mRectHeight,
              i * mScaleMargin + mInitDistance, mRectHeight - mLineHeight / 2 - mTextLineMargin,
              paint);
        }
      } else {//画滚动到末尾余数 30200
        if (mPage == (mTotalTextList.size() - 20) / 10 - 1 || mTotalTextList.size() < 40) {
          if ((mLastInstance / mScaleMargin) > 0
              && (mLastInstance / mScaleMargin) > i - (mTextList.size() * 10 - 9)) {
            paint.setColor(Color.GRAY);
            canvas.drawLine(i * mScaleMargin + mInitDistance, mRectHeight,
                i * mScaleMargin + mInitDistance, mRectHeight - mLineHeight / 2 - mTextLineMargin,
                paint);
          }
        }
      }
    }
  }

  private void onDrawLine(Canvas canvas, Paint paint) {
    paint.setStrokeWidth(2);
    canvas.drawLine(mInitDistance, mRectHeight, mScaleWidth + mInitDistance, mRectHeight, paint);
  }

  /**
   * 使用Scroller的时候需要重写该方法
   */
  @Override
  public void computeScroll() {
    if (mScroller.computeScrollOffset()) {
      scrollTo(mScroller.getCurrX(), 0);
      postInvalidate();
    }
  }

  private void smoothScrollBy(int dx, int dy) {
    if (mScroller.getFinalX() >= mScaleMargin * 10 * 10 * 2 - 10 * mScaleMargin
        && dx > 0) {//向左滚动，判断是否滚动到第三部分起始位置
      if (!notifyDataChanged(0)) {
        mScroller.startScroll(mScroller.getFinalX(), mScroller.getFinalY(), dx, dy);
      }
    } else if (mScroller.getFinalX() <= mScaleMargin * 10 * 10 - 10 * mScaleMargin
        && dx < 0) {///向右滚动，判断是否滚动到第二部分开始位置
      if (!notifyDataChanged(1)) {
        mScroller.startScroll(mScroller.getFinalX(), mScroller.getFinalY(), dx, dy);
      }
    } else {//否则根据手指滚动
      mScroller.startScroll(mScroller.getFinalX(), mScroller.getFinalY(), dx, dy);
    }
    postInvalidate();
  }

  public interface OnScrollListener {
    void onScaleScrollChanged(long scale);
  }

  /**
   * 设置当前位置
   *
   * @param scale 刻度
   */
  public void setNowScale(float scale) {
    float i = scale * mScaleMargin;
    int maxPage = (mTotalTextList.size() - 20) / 10 - 1;
    mPage = 0;
    if (mTotalTextList.size() < 40) {//如果总数小于40 不需要处理多页
      mPage = 0;
      smoothScrollBy((int) (i - mScroller.getFinalX()), 0);
      return;
    } else if (i < mScaleMargin * 10 * 10 * 2 - mScaleMargin * 10) {//判断刻度是否在第一页
      mPage = 0;
    } else if (scale >= 10 * 10 + maxPage * 10 * 10) {//判断刻度是否在最后一页
      mPage = maxPage;
    } else {
      mPage = (int) (scale / 100 - 1);
    }
    if (scale * mUnit >= mFinalMoney) {//如果要设置的刻度大于集合里的最大值 则只滚动到末尾
      mPage = maxPage;
      mTextList.clear();
      for (int j = mPage * 10; j < mTotalTextList.size(); j++) {
        mTextList.add(mTotalTextList.get(j));
      }
      mScaleWidth = (mTextList.size() * 10 - 10) * mScaleMargin + mLastInstance;
      smoothScrollBy((int) (mScaleWidth - mScroller.getFinalX()), 0);
    } else {
      if (mPage == (mTotalTextList.size() - 20) / 10 - 1) {//判断是否是最后一页
        if (mTotalTextList.size() >= mPage * 10 + 30) {
          mTextList.clear();
          for (int j = mPage * 10; j < mTotalTextList.size(); j++) {//更新刻度
            mTextList.add(mTotalTextList.get(j));
          }
          mScaleWidth = (mTextList.size() * 10 - 10) * mScaleMargin + mLastInstance;//加上余数的宽度
          smoothScrollBy((int) (i - ((mPage + 1) * 10 * 10 * mScaleMargin) + 10 * 10 * mScaleMargin
              - mScroller.getFinalX()), 0);
        }
      } else {
        if (mTotalTextList.size() >= mPage * 10 + 30) {
          mTextList.clear();
          for (int j = mPage * 10; j < mPage * 10 + 30; j++) {//更新刻度
            mTextList.add(mTotalTextList.get(j));
          }
          mScaleWidth = (mTextList.size() * 10 - 10) * mScaleMargin;
          smoothScrollBy((int) (i - ((mPage + 1) * 10 * 10 * mScaleMargin) + 10 * 10 * mScaleMargin
              - mScroller.getFinalX()), 0);
        }
      }
    }
  }

  /**
   * 初始化数据
   *
   * @param startMoney 开始金额
   * @param finalMoney 最终金额
   * @param unit 每个刻度单位
   * @param listener 滚动监听
   */
  public void bindMoneyData(int startMoney, int finalMoney, int unit, OnScrollListener listener) {
    if (mTotalTextList != null && mTotalTextList.size() > 0) {
      refresh(startMoney, finalMoney);
    } else {
      mPage = 0;
      mUnit = unit;
      mStartMoney = startMoney / unit;
      mTextList = new ArrayList<>();
      mTotalTextList = new ArrayList<>();
      mFinalMoney = finalMoney;
      for (int i = 0; i < (finalMoney - startMoney) / (unit * 10) + 1; i++) {
        if (i < 30 || (finalMoney - startMoney) / (unit * 10) + 1 < 40) {//当前显示刻度数
          mTextList.add(String.valueOf(i * unit * 10 + startMoney));
        }
        mTotalTextList.add(String.valueOf(i * unit * 10 + startMoney));//总共刻度数
      }
      mLastInstance = ((finalMoney - startMoney) % (unit * 10)) / 100 * mScaleMargin;//余数刻度
      if (mTotalTextList.size() < 40) {
        mScaleWidth = (mTextList.size() * 10 - 10) * mScaleMargin + mLastInstance;
      } else {
        mScaleWidth = (mTextList.size() * 10 - 10) * mScaleMargin;
      }
      mListener = listener;
      postInvalidate();
    }
  }

  /**
   * 判断更新当前页
   *
   * @param type 0左滑  1右滑
   * @return 是否处理了滚动到对应页
   */
  private boolean notifyDataChanged(int type) {
    if (mTotalTextList.size() < 40) {
      return false;
    }
    if (type == 0) {//向前移动
      if ((mTotalTextList.size() - 20) / 10 - 1 == 0) {
        return false;
      }
      if (mPage < (mTotalTextList.size() - 20) / 10 - 1) {
        mPage++;
        if (mPage == (mTotalTextList.size() - 20) / 10 - 1) {
          if (mTotalTextList.size() >= mPage * 10 + 30) {
            mTextList.clear();
            for (int i = mPage * 10; i < mTotalTextList.size(); i++) {
              mTextList.add(mTotalTextList.get(i));
            }
            mScaleWidth = (mTextList.size() * 10 - 10) * mScaleMargin + mLastInstance;
            mScroller.startScroll(mScroller.getFinalX(), mScroller.getFinalY(),
                -mScroller.getFinalX() + mScaleMargin * 10 * 10 - 10 * mScaleMargin,
                mScroller.getFinalY());
            return true;
          }
        } else {
          if (mTotalTextList.size() >= mPage * 10 + 30) {
            mTextList.clear();
            for (int i = mPage * 10; i < mPage * 10 + 30; i++) {
              mTextList.add(mTotalTextList.get(i));
            }
            mScaleWidth = (mTextList.size() * 10 - 10) * mScaleMargin;
            mScroller.startScroll(mScroller.getFinalX(), mScroller.getFinalY(),
                -mScroller.getFinalX() + mScaleMargin * 10 * 10 - 10 * mScaleMargin,
                mScroller.getFinalY());
            return true;
          }
        }
      }
    } else {//向后移动
      if (mPage > 0) {
        mPage--;
        if (mTotalTextList.size() > mPage * 10 + 30) {
          mTextList.clear();
          for (int i = mPage * 10; i < mPage * 10 + 30; i++) {
            mTextList.add(mTotalTextList.get(i));
          }
          mScaleWidth = (mTextList.size() * 10 - 10) * mScaleMargin;
          mScroller.startScroll(mScroller.getFinalX(), mScroller.getFinalY(),
              mScaleMargin * 10 * 10, mScroller.getFinalY());
          return true;
        }
      }
    }
    return false;
  }

  /**
   * 刷新操作
   */
  public void refresh(final int startMoney, final int finalMoney) {
    new Thread(new Runnable() {//防止数据量过多导致阻塞主线程
      @Override
      public void run() {
        mPage = 0;
        mStartMoney = startMoney / mUnit;
        mFinalMoney = finalMoney;
        mTextList.clear();
        mTotalTextList.clear();
        for (int i = 0; i < (finalMoney - startMoney) / (mUnit * 10) + 1; i++) {
          if (i < 30 || (finalMoney - startMoney) / (mUnit * 10) + 1 < 40) {//当前显示刻度数
            mTextList.add(String.valueOf(i * mUnit * 10 + startMoney));
          }
          mTotalTextList.add(String.valueOf(i * mUnit * 10 + startMoney));//总共刻度数
        }
        mLastInstance = ((finalMoney - startMoney) % (mUnit * 10)) / 100 * mScaleMargin;//余数刻度
        if (mTotalTextList.size() < 40) {
          mScaleWidth = (mTextList.size() * 10 - 10) * mScaleMargin + mLastInstance;
        } else {
          mScaleWidth = (mTextList.size() * 10 - 10) * mScaleMargin;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
          @Override
          public void run() {
            postInvalidate();
          }
        });
      }
    });
  }

  /**
   * 设置每个刻度间距
   *
   * @param margin 间距
   * @return 返回当前view 链式编程
   */
  public ScrollDividingRuleView setScaleMargin(int margin) {
    mScaleMargin = margin;
    mRectHeight = (int) (mLineHeight + mTextSize + mTextLineMargin + 20);
    return this;
  }

  /**
   * 设置文字和刻度线的间距
   */
  public ScrollDividingRuleView setTextLineMargin(int textLineMargin) {
    mTextLineMargin = textLineMargin;
    mRectHeight = (int) (mLineHeight + mTextSize + mTextLineMargin + 20);
    return this;
  }

  /**
   * 设置文字和刻度线的间距(滚动有问题)
   */
  public ScrollDividingRuleView setInitPosition(int position) {
    mInitPosition=position;
    return this;
  }
}
