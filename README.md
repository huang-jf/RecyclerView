# RecyclerView
RecyclerView 封装头部水滴刷新动画加在更多

项目使用RecyclerView代替ListView，为了方便开发封装了适配器并给RecyclerView增加了常用方法。这里感谢[XRecyclerView](https://github.com/jianghejie/XRecyclerView)的作者，给了我很大帮助。


![水滴刷新效果](http://img.blog.csdn.net/20160827232338794)

## 对XRecyclerView进行的修改



在实际使用过程中，和开发需求有点差别。进行下面的修改操作

 - 修改：在禁止刷新、加载更多等情况下，空视图展示的判定有失误
 - 修改：当没有更多数据是上滑动不加载数据
 - 修改：没有新数据时加入时显示“无更多”的FootView
 - 修改：改变手动调用加载完成、刷新完成的（需要Adapter的配合）
 - 修改：在最后条目可见的情况下，短距离下拉控件也能触发加载更多行为，修改后不触发加载更多行为



## RecyclerView 通用Adapter的封装

在使用RecyclerView等控件的时候，肯定写个抽象适配器用来简化代码、减少工作量。基本按照需求改变了XRecyclerView和加入适配器后，那么使用起来估摸着肯定会出现问题，发现在添加头部的情况下，增添数据时，**Item视图加入RecyclerView的位置**有问题。

看下适配器中添加数据的方法：

```
public void addDatas(List<T> datas) {
        if (datas == null) datas = new ArrayList<>();
        this.mDataSource.addAll(datas);
        this.notifyItemRangeInserted(this.mDataSource.size() - datas.size(), datas.size());
    }
```

在以前使用时这个添加数据的方法是没问题的，数据能加到准确的位置上去。但现在配合使用之后
发现数据加入的位置确实有问题，在添加头部View的时候添加动画位置是错的。

我们看下代码 **XRecyclerView**中的**setAdapter**：

```
@Override
    public void setAdapter(Adapter adapter) {
        mWrapAdapter = new WrapAdapter(adapter);
        super.setAdapter(mWrapAdapter);
        mWrapAdapter.registerAdapterDataObserver(mDataObserver);
        if (adapter.getItemCount() != 0)
            mDataObserver.onChanged();
    }
```

知道了有问题，不用着急，也不用担心。第一件事是多点点，多看看，然后你就会发现，后面会有更多的问题。简单的调试和修改过后，果不其然。发现每一次数据变化会进入AdapterDataObservable的同一方法两次。第一次是有用的，第二次是用来庆祝第一次有用的。

经过查看， **XRecyclerView**在封装进入**头部、脚部**等View的时候，采用了进一步包装Adapter。所以并不是直接使用**setAdapter**方法传入的适配器对象。那数据变化时我依然在封装的Adapter里面调用notify方法，这不是很靠谱。在**RecyclerView**的**setAdapter**方法里面有一句代码：

```
        adapter.registerAdapterDataObserver(mDataObserver);
```
传入的Adapter对象注册了个适配器数据观察者，在对象调用notify方法的时候这个观察者会被触发并回调执行方法，而在这个回调的方法里面包装后的WrapAdapter又去notify了，又去notify了，又去notify了。问题是去了也纠正不了第一次的错误。

到这里可以找到解决思路了，就是第一次不去notify，找方法让WrapAdapter去notify，或者反过来。第一种去做了发现数据变更了，但是如果是在列表中加入数据没有动画效果，看了源码后发是**AdapterDataObservable**没写好。最后采取了反过来的方法去实现，只要解决数据索引就行了，原因后面讲。


跟着源码看下notify的打开方式，这里选择**notifyItemRemoved(int)**方法作为突破口，其他的方法类似。下文用*notify(...)*指代方法 **notifyItemRemoved(int)**。

进如*notify(...)*看代码
```
    public static abstract class Adapter<VH extends ViewHolder> {
    ...

		public final void notifyItemRemoved(int position) {
            mObservable.notifyItemRangeRemoved(position, 1);
        }
	
	...
    }
```

激活动作是由AdapterDataObservable对象进行的，继续进入查看

```
    static class AdapterDataObservable extends Observable<AdapterDataObserver> {
	    ...
		public void notifyItemRangeRemoved(int positionStart, int itemCount) {
		            for (int i = mObservers.size() - 1; i >= 0; i--) {
		                mObservers.get(i).onItemRangeRemoved(positionStart, itemCount);
		            }
		}
		....
	}
```
这个操作会共享给所有的**AdapterDataObserver**对象去回调数据发生的改变，那么我们去看看RecyclerView默认给我门添加的那个**AdapterDataObserver**是怎么实现的。在和我们自己实现的对比，就可以找出问题并实现了。

```
	private class RecyclerViewDataObserver extends AdapterDataObserver {
        ...
        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            assertNotInLayoutOrScroll(null);
            if (mAdapterHelper.onItemRangeRemoved(positionStart, itemCount)) {
                triggerUpdateProcessor();
            }
        }
        ...
    }
```
其他的不看，就看这个改变的。然后就看了*triggerUpdateProcessor()*方法，如此耀眼。开启数据变化时的Item加入或消失的动画效果。如果不做特效的话，是没有必要自己去实现添加动画的，所以采取了第二种解决方案，这样子也增加了控件和适配器之间的耦合度。

*triggerUpdateProcessor()* 方法的代码：
```
		void triggerUpdateProcessor() {
            if (mPostUpdatesOnAnimation && mHasFixedSize && mIsAttached) {
                ViewCompat.postOnAnimation(RecyclerView.this, mUpdateChildViewsRunnable);
            } else {
                mAdapterUpdateDuringMeasure = true;
                requestLayout();
            }
        }
```
 然后自己去点吧，这不是本文重点。最后我还是准备在传入的Adapter里面调用激活方法，但是先进行数据变化是索引的计算。所以进行如下修改：
```
	...
	
	private XRecyclerView.WrapAdapterDataManager dataManager;

    public void setDataManager(XRecyclerView.WrapAdapterDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public void setDatas(List<T> datas) {
        if (datas == null) datas = new ArrayList<>();
        this.mDataSource.clear();
        this.mDataSource.addAll(datas);
        if (dataManager == null) {
            this.notifyDataSetChanged();
        } else {
            dataManager.notifyDataSetChanged1();
        }
    }

	public void addDatas(List<T> datas) {
        if (datas == null) datas = new ArrayList<>();
        this.mDataSource.addAll(datas);
        if (dataManager == null) {
            this.notifyItemRangeInserted(this.mDataSource.size() - datas.size() + 1, datas.size());
        } else {
            dataManager.notifyItemRangeInserted1(this.mDataSource.size() - datas.size() + 1, datas.size());
        }
    }
    ...
```

有一个XRecyclerView里面的接口对象，当它不为null的时候我们就调用他的方法取代Adapter的激活方法，当它为null的时候就调用Adapter的激活方法。那他什么时候赋值呢？

```
	public class WrapAdapter extends Adapter<ViewHolder> implements WrapAdapterDataManager {
		...
		public WrapAdapter(Adapter adapter) {
            this.adapter = adapter;
            if (this.adapter instanceof CommomRecyclerAdapter) {
                ((CommomRecyclerAdapter) this.adapter).setDataManager(this);
            }
        }
        ...
          @Override
        public void notifyItemRangeInserted1(int positionStart, int itemCount) {
	        //这里进行激活条目的计算，然后在用传入的Adapter对象激活数据
            adapter.notifyItemRangeInserted(positionStart, itemCount);
        }
        ...
	}
```
这个是封装适配器WrapAdapter的构造方法。检测如果传入的Adapter是我们封装的*CommomRecyclerAdapter*的时候，给那个对象赋值。并且在**WrapAdapterDataManager**抽象方法的实现里面直接利用传入的Adapter对象去调用激活，当然之前的计算好数据变化的索引位置。这样也可以不用去注册自己实现的**AdapterDataObserver**了，也不会因为一次数据变化多次调用数据观察者同一方法。
这种改动无疑增加了适配器和RecyclerView之间的耦合度，经过这次对XRecyclerView的使用和改造，我已经有了想法，不增加耦合度的情况下达到目的，下次去定义RecyclerView控件时实现。


## 水滴刷新动画的绘制
[详细思路点击](http://blog.csdn.net/uedtianji/article/details/42250665?utm_source=tuicool&utm_medium=referral)

![水滴动画的设计](http://img.blog.csdn.net/20160827221135767)

部分代码

```
@Override
    protected void onDraw(Canvas canvas) {
        makeBezierPath();
        //画顶部
        mPaint.setColor(Color.parseColor("#2abb9c"));
        canvas.drawPath(mPath, mPaint);
        canvas.drawCircle(topCircle.getX(), topCircle.getY(), topCircle.getRadius(), mPaint);
        //画底部
        mPaint.setColor(Color.parseColor("#2abb9c"));
        canvas.drawCircle(bottomCircle.getX(), bottomCircle.getY(), bottomCircle.getRadius(), mPaint);
        RectF bitmapArea = new RectF(
                topCircle.getX() - 0.5f * topCircle.getRadius(),
                topCircle.getY() - 0.5f * topCircle.getRadius(),
                topCircle.getX() + 0.5f * topCircle.getRadius(),
                topCircle.getY() + 0.5f * topCircle.getRadius());
        canvas.drawBitmap(arrowBitmap, null, bitmapArea, mPaint);
        super.onDraw(canvas);
    }


    private void makeBezierPath() {
        mPath.reset();
        //获取两圆的两个切线形成的四个切点
        double angle = getAngle();
        float top_x1 = (float) (topCircle.getX() - topCircle.getRadius() * Math.cos(angle));
        float top_y1 = (float) (topCircle.getY() + topCircle.getRadius() * Math.sin(angle));

        float top_x2 = (float) (topCircle.getX() + topCircle.getRadius() * Math.cos(angle));
        float top_y2 = top_y1;

        float bottom_x1 = (float) (bottomCircle.getX() - bottomCircle.getRadius() * Math.cos(angle));
        float bottom_y1 = (float) (bottomCircle.getY() + bottomCircle.getRadius() * Math.sin(angle));

        float bottom_x2 = (float) (bottomCircle.getX() + bottomCircle.getRadius() * Math.cos(angle));
        float bottom_y2 = bottom_y1;

        mPath.moveTo(topCircle.getX(), topCircle.getY());

        mPath.lineTo(top_x1, top_y1);

        mPath.quadTo((bottomCircle.getX() - bottomCircle.getRadius()),
                (bottomCircle.getY() + topCircle.getY()) / 2,
                bottom_x1,
                bottom_y1);
        mPath.lineTo(bottom_x2, bottom_y2);

        mPath.quadTo((bottomCircle.getX() + bottomCircle.getRadius()),
                (bottomCircle.getY() + top_y2) / 2,
                top_x2,
                top_y2);
        mPath.close();
    }
```