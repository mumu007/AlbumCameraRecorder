package com.zhongjh.cameraviewsoundrecorder.album.ui.preview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zhongjh.cameraviewsoundrecorder.R;
import com.zhongjh.cameraviewsoundrecorder.album.entity.IncapableCause;
import com.zhongjh.cameraviewsoundrecorder.album.entity.Item;
import com.zhongjh.cameraviewsoundrecorder.album.entity.SelectionSpec;
import com.zhongjh.cameraviewsoundrecorder.album.model.SelectedItemCollection;
import com.zhongjh.cameraviewsoundrecorder.album.ui.preview.selectedpreview.adapter.PreviewPagerAdapter;
import com.zhongjh.cameraviewsoundrecorder.album.ui.previewitem.PreviewItemFragment;
import com.zhongjh.cameraviewsoundrecorder.album.utils.PhotoMetadataUtils;
import com.zhongjh.cameraviewsoundrecorder.album.widget.CheckRadioView;
import com.zhongjh.cameraviewsoundrecorder.album.widget.CheckView;
import com.zhongjh.cameraviewsoundrecorder.album.widget.PreviewViewPager;
import com.zhongjh.cameraviewsoundrecorder.utils.VersionUtils;
import com.zhongjh.cameraviewsoundrecorder.widget.IncapableDialog;

/**
 * 预览的基类
 */
public class BasePreviewActivity extends AppCompatActivity implements View.OnClickListener,
        ViewPager.OnPageChangeListener {

    public static final String EXTRA_DEFAULT_BUNDLE = "extra_default_bundle";
    public static final String EXTRA_RESULT_BUNDLE = "extra_result_bundle";
    public static final String EXTRA_RESULT_APPLY = "extra_result_apply";
    public static final String EXTRA_RESULT_ORIGINAL_ENABLE = "extra_result_original_enable";
    public static final String CHECK_STATE = "checkState";

    protected final SelectedItemCollection mSelectedCollection = new SelectedItemCollection(this);
    protected SelectionSpec mSpec;

    protected PreviewPagerAdapter mAdapter;

    protected boolean mOriginalEnable;      // 是否原图

    protected int mPreviousPos = -1;    // 当前预览的图片的索引

    protected ViewHolder mViewHolder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(SelectionSpec.getInstance().themeId);  // 获取样式
        super.onCreate(savedInstanceState);
        if (!SelectionSpec.getInstance().hasInited) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        setContentView(R.layout.activity_media_preview_zjh);
        if (VersionUtils.hasKitKat()) {
            // 使用沉倾状态栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        mSpec = SelectionSpec.getInstance();
        if (mSpec.needOrientationRestriction()) {
            // 设置旋转模式
            setRequestedOrientation(mSpec.orientation);
        }

        if (savedInstanceState == null) {
            // 初始化别的界面传递过来的数据
            mSelectedCollection.onCreate(getIntent().getBundleExtra(EXTRA_DEFAULT_BUNDLE));
            mOriginalEnable = getIntent().getBooleanExtra(EXTRA_RESULT_ORIGINAL_ENABLE, false);
        } else {
            // 初始化缓存的数据
            mSelectedCollection.onCreate(savedInstanceState);
            mOriginalEnable = savedInstanceState.getBoolean(CHECK_STATE);
        }

        mViewHolder = new ViewHolder(this);

        mAdapter = new PreviewPagerAdapter(getSupportFragmentManager(), null);
        mViewHolder.pager.setAdapter(mAdapter);
        mViewHolder.check_view.setCountable(mSpec.countable);

        initListener();
    }

    /**
     * 所有事件
     */
    private void initListener(){
        // 返回
        mViewHolder.button_back.setOnClickListener(this);
        // 确认
        mViewHolder.button_apply.setOnClickListener(this);
        // 多图时滑动事件
        mViewHolder.pager.addOnPageChangeListener(this);
        // 右上角选择事件
        mViewHolder.check_view.setOnClickListener(v -> {
            Item item = mAdapter.getMediaItem(mViewHolder.pager.getCurrentItem());
            if (mSelectedCollection.isSelected(item)) {
                mSelectedCollection.remove(item);
                if (mSpec.countable) {
                    mViewHolder.check_view.setCheckedNum(CheckView.UNCHECKED);
                } else {
                    mViewHolder.check_view.setChecked(false);
                }
            } else {
                if (assertAddSelection(item)) {
                    mSelectedCollection.add(item);
                    if (mSpec.countable) {
                        mViewHolder.check_view.setCheckedNum(mSelectedCollection.checkedNumOf(item));
                    } else {
                        mViewHolder.check_view.setChecked(true);
                    }
                }
            }
            updateApplyButton();

            if (mSpec.onSelectedListener != null) {
                // 触发选择的接口事件
                mSpec.onSelectedListener.onSelected(
                        mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString());
            }
        });
        // 原图事件
        mViewHolder.originalLayout.setOnClickListener(v -> {
            int count = countOverMaxSize();
            if (count > 0) {
                IncapableDialog incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.error_over_original_count, count, mSpec.originalMaxSize));
                incapableDialog.show(getSupportFragmentManager(),
                        IncapableDialog.class.getName());
                return;
            }

            mOriginalEnable = !mOriginalEnable;
            mViewHolder.original.setChecked(mOriginalEnable);
            if (!mOriginalEnable) {
                mViewHolder.original.setColor(Color.WHITE);
            }


            if (mSpec.onCheckedListener != null) {
                mSpec.onCheckedListener.onCheck(mOriginalEnable);
            }
        });
        updateApplyButton();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mSelectedCollection.onSaveInstanceState(outState);
        outState.putBoolean("checkState", mOriginalEnable);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        sendBackResult(false);
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_back) {
            onBackPressed();
        } else if (v.getId() == R.id.button_apply) {
            sendBackResult(true);
            finish();
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    /**
     * 滑动事件
     * @param position 索引
     */
    @Override
    public void onPageSelected(int position) {
        PreviewPagerAdapter adapter = (PreviewPagerAdapter) mViewHolder.pager.getAdapter();
        if (mPreviousPos != -1 && mPreviousPos != position) {
            ((PreviewItemFragment) adapter.instantiateItem(mViewHolder.pager, mPreviousPos)).resetView();

            Item item = adapter.getMediaItem(position);
            if (mSpec.countable) {
                int checkedNum = mSelectedCollection.checkedNumOf(item);
                mViewHolder.check_view.setCheckedNum(checkedNum);
                if (checkedNum > 0) {
                    mViewHolder.check_view.setEnabled(true);
                } else {
                    mViewHolder.check_view.setEnabled(!mSelectedCollection.maxSelectableReached());
                }
            } else {
                boolean checked = mSelectedCollection.isSelected(item);
                mViewHolder.check_view.setChecked(checked);
                if (checked) {
                    mViewHolder.check_view.setEnabled(true);
                } else {
                    mViewHolder.check_view.setEnabled(!mSelectedCollection.maxSelectableReached());
                }
            }
            updateSize(item);
        }
        mPreviousPos = position;
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    /**
     * 更新确定按钮状态
     */
    private void updateApplyButton() {
        // 获取已选的图片
        int selectedCount = mSelectedCollection.count();
        if (selectedCount == 0) {
            // 禁用
            mViewHolder.button_apply.setText(R.string.button_sure_default);
            mViewHolder.button_apply.setEnabled(false);
        } else if (selectedCount == 1 && mSpec.singleSelectionModeEnabled()) {
            // 如果只选择一张或者配置只能选一张，或者不显示数字的时候。启用，不显示数字
            mViewHolder.button_apply.setText(R.string.button_sure_default);
            mViewHolder.button_apply.setEnabled(true);
        } else {
            // 启用，显示数字
            mViewHolder.button_apply.setEnabled(true);
            mViewHolder.button_apply.setText(getString(R.string.button_sure, selectedCount));
        }

        // 判断是否开启原图
        if (mSpec.originalable) {
            // 显示
            mViewHolder.originalLayout.setVisibility(View.VISIBLE);
            updateOriginalState();
        } else {
            // 隐藏
            mViewHolder.originalLayout.setVisibility(View.GONE);
        }
    }

    /**
     * 更新原图按钮状态
     */
    private void updateOriginalState() {
        // 设置原图按钮根据配置来
        mViewHolder.original.setChecked(mOriginalEnable);
        if (!mOriginalEnable) {
            mViewHolder.original.setColor(Color.WHITE);
        }

        if (countOverMaxSize() > 0) {
            // 如果开启了原图功能
            if (mOriginalEnable) {
                // 弹框提示取消原图
                IncapableDialog incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.error_over_original_size, mSpec.originalMaxSize));
                incapableDialog.show(getSupportFragmentManager(),
                        IncapableDialog.class.getName());
                // 去掉原图按钮的选择状态
                mViewHolder.original.setChecked(false);
                mViewHolder.original.setColor(Color.WHITE);
                mOriginalEnable = false;
            }
        }
    }

    /**
     * 获取当前超过限制原图大小的数量
     * @return 数量
     */
    private int countOverMaxSize() {
        int count = 0;
        int selectedCount = mSelectedCollection.count();
        for (int i = 0; i < selectedCount; i++) {
            Item item = mSelectedCollection.asList().get(i);
            if (item.isImage()) {
                float size = PhotoMetadataUtils.getSizeInMB(item.size);
                if (size > mSpec.originalMaxSize) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 如果当前item是gif就显示多少M的文本
     * 如果当前item是video就显示播放按钮
     * @param item 当前图片
     */
    @SuppressLint("SetTextI18n")
    protected void updateSize(Item item) {
        if (item.isGif()) {
            mViewHolder.size.setVisibility(View.VISIBLE);
            mViewHolder.size.setText(PhotoMetadataUtils.getSizeInMB(item.size) + "M");
        } else {
            mViewHolder.size.setVisibility(View.GONE);
        }

        if (item.isVideo()) {
            mViewHolder.originalLayout.setVisibility(View.GONE);
        } else if (mSpec.originalable) {
            mViewHolder.originalLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 设置返回值
     * @param apply 是否同意
     */
    protected void sendBackResult(boolean apply) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESULT_BUNDLE, mSelectedCollection.getDataWithBundle());
        intent.putExtra(EXTRA_RESULT_APPLY, apply);
        intent.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
        setResult(Activity.RESULT_OK, intent);
    }

    /**
     * 处理窗口
     * @param item 当前图片
     * @return 为true则代表符合规则
     */
    private boolean assertAddSelection(Item item) {
        IncapableCause cause = mSelectedCollection.isAcceptable(item);
        IncapableCause.handleCause(this, cause);
        return cause == null;
    }

    public static class ViewHolder {
        public Activity activity;
        PreviewViewPager pager;
        TextView button_back;
        public CheckRadioView original;
        public LinearLayout originalLayout;
        public TextView size;
        public TextView button_apply;
        public FrameLayout bottom_toolbar;
        public CheckView check_view;

        ViewHolder(Activity activity) {
            this.activity = activity;
            this.pager = activity.findViewById(R.id.pager);
            this.button_back = activity.findViewById(R.id.button_back);
            this.original = activity.findViewById(R.id.original);
            this.originalLayout = activity.findViewById(R.id.originalLayout);
            this.size = activity.findViewById(R.id.size);
            this.button_apply = activity.findViewById(R.id.button_apply);
            this.bottom_toolbar = activity.findViewById(R.id.bottom_toolbar);
            this.check_view = activity.findViewById(R.id.check_view);
        }

    }
}