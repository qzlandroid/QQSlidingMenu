package cn.jit.qqslidingmenu;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.CycleInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Random;

import cn.jit.qqslidingmenu.ui.DragLayout;
import cn.jit.qqslidingmenu.ui.MyLinearLayout;
import cn.jit.qqslidingmenu.util.Cheeses;
import cn.jit.qqslidingmenu.util.Utils;

public class MainActivity extends AppCompatActivity {

    private ListView mLeftList;
    private ListView mMainList;
    private ImageView mHeaderImage;
    private MyLinearLayout mLinearLayout;
    private DragLayout mDragLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        initView();
        initListener();
    }

    private void init() {
        mLeftList = (ListView) findViewById(R.id.lv_left);
        mMainList = (ListView) findViewById(R.id.lv_main);
        mHeaderImage = (ImageView) findViewById(R.id.iv_header);
        mLinearLayout = (MyLinearLayout) findViewById(R.id.mll);
        mDragLayout = (DragLayout) findViewById(R.id.dl);
    }

    private void initView() {
        // 设置引用
        mLinearLayout.setDraglayout(mDragLayout);
        mLeftList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                Cheeses.sCheeseStrings) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView mText = ((TextView) view);
                mText.setTextColor(Color.WHITE);
                return view;
            }
        });
        mMainList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                Cheeses.NAMES));
    }

    private void initListener() {
        mDragLayout.setOnDragStatusChangeListener(new DragLayout.OnDragStatusChangeListener() {
            @Override
            public void onClose() {
                Utils.showToast(MainActivity.this, "onClose");
                // 让图标晃动
                ObjectAnimator mAnim = ObjectAnimator.ofFloat(mHeaderImage, "translationX", 15.0f);
                mAnim.setInterpolator(new CycleInterpolator(4));
                mAnim.setDuration(500);
                mAnim.start();
            }

            @Override
            public void onOpen() {
                Utils.showToast(MainActivity.this, "onOpen");
                // 左面板ListView随机设置一个条目
                Random random = new Random();
                int nextInt = random.nextInt(25);
                mLeftList.smoothScrollToPosition(nextInt);
            }

            @Override
            public void onDraging(float percent) {
                mHeaderImage.setAlpha(1 - percent);
            }
        });
    }

}

