# ViewsFlipper
模仿淘宝、京东消息轮播控件，利用了RecylerView.Adapter，用法和ReclerView的用法完全一致，非常容易扩展。

# Demo示例
<img width="360" height="640" src=https://github.com/Mr1ee/ViewsFlipper/blob/master/screenshots/screenshot1.gif/>
<img width="360" height="640" src=https://github.com/Mr1ee/ViewsFlipper/blob/master/screenshots/screenshot2.gif/>
# 使用方法
在XML中设置如下。其中flipDuration表示动画时长， flipInterval是轮播间隔时间，flipInterval一定要大于flipDuration，否则会抛出异常。
```java
    <fresh.lee.viewsflipper.ViewsFlipper
        android:id="@+id/flipper"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:flipDuration="500"
        app:flipInterval="2500"/>
```

Activity中Example，
```java
    private List<Pair<Integer, String>> items = new ArrayList<>();
    items.add(new Pair<>(R.drawable.ic_ac_unit_black_24dp, "感受停在我发端的指尖"));
    items.add(new Pair<>(R.drawable.ic_android_black_24dp, "如何瞬间 冻结时间"));
    items.add(new Pair<>(R.drawable.ic_brightness_3_black_24dp, "记住望着我坚定的双眼"));
    items.add(new Pair<>(R.drawable.ic_cloud_black_24dp, "也许已经 没有明天"));
    final ViewsFlipper flipper = findViewById(R.id.flipper);
    final FlipperAdapter adapter = new FlipperAdapter(items);
    flipper.setAdapter(adapter);
    flipper.setOrientation(RecyclerView.HORIZONTAL);
    flipper.startFlipping();
    
    class FlipperAdapter extends RecyclerView.Adapter<VH> {
        private List<Pair<Integer, String>> list;

        FlipperAdapter(List<Pair<Integer, String>> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_flipper_child, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.bind(list.get(position).first, list.get(position).second);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    class VH extends RecyclerView.ViewHolder {
        private TextView tvLyric;
        private ImageView iv;

        VH(@NonNull View itemView) {
            super(itemView);
            tvLyric = itemView.findViewById(R.id.tv_child);
            iv = itemView.findViewById(R.id.iv_child);
        }

        void bind(int drawableId, String lyric) {
            iv.setImageResource(drawableId);
            tvLyric.setText(lyric);
        }
    }
```
从代码中就看出使用非常简单，用法完全和RecyclerView一致，而且更重要的是，和RecyclerView一样，完全可以自己定制各种Child View。
1-支持数据的动态更新，和RecyclerView一样，改变数据之后需要调用Adapter.notifyDataSetChanged；
2-支持设置滚动方向，支持垂直滚动和水平滚动。只需要调用ViewsFlipper.setOrientation即可，参数是RecyclerView.HORIZONTAL或者RecyclerView.VERTICAL。

# API
Function | 作用  
-|-
setAdapter|设置数据源
startFlipping|开始轮播
stopFlipping|停止轮播
setFlipInterval|设置轮播间隔时间
getFlipInterval|获取轮播间隔时间
setFlipDuration|设置滚动动画时间
getFlipDuration|获取滚动动画时间
setOrientation|设置滚动方向
getOrientation|获取当前滚动方向
