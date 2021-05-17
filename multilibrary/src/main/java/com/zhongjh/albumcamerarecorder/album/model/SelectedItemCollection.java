package com.zhongjh.albumcamerarecorder.album.model;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.zhongjh.albumcamerarecorder.R;

import gaode.zhongjh.com.common.entity.IncapableCause;
import gaode.zhongjh.com.common.entity.MultiMedia;

import com.zhongjh.albumcamerarecorder.album.utils.PhotoMetadataUtils;
import com.zhongjh.albumcamerarecorder.settings.AlbumSpec;
import com.zhongjh.albumcamerarecorder.settings.GlobalSpec;
import com.zhongjh.albumcamerarecorder.utils.MultiMediaUtils;
import com.zhongjh.albumcamerarecorder.utils.PathUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static gaode.zhongjh.com.common.enums.Constant.IMAGE;
import static gaode.zhongjh.com.common.enums.Constant.VIDEO;

/**
 * 选择的数据源
 *
 * @author zhongjh
 * @date 2018/8/28
 */
public class SelectedItemCollection {

    /**
     * 数据源的标记
     */
    public static final String STATE_SELECTION = "state_selection";
    public static final String STATE_COLLECTION_TYPE = "state_collection_type";

    /**
     * 空的数据类型
     */
    public static final int COLLECTION_UNDEFINED = 0x00;
    /**
     * 图像数据类型
     */
    public static final int COLLECTION_IMAGE = 0x01;
    /**
     * 视频数据类型
     */
    public static final int COLLECTION_VIDEO = 0x01 << 1;
    /**
     * 图像和视频混合类型
     */
    private static final int COLLECTION_MIXED = COLLECTION_IMAGE | COLLECTION_VIDEO;

    private final Context mContext;
    /**
     * 数据源
     */
    private Set<MultiMedia> mItems;
    /**
     * 当前选择的所有类型，列表如果包含了图片和视频，就会变成混合类型
     */
    private int mCollectionType = COLLECTION_UNDEFINED;
    /**
     * 当前选择的视频数量
     */
    private int mSelectedVideoCount;
    /**
     * 当前选择的图片数量
     */
    private int mSelectedImageCount;

    public SelectedItemCollection(Context context) {
        mContext = context;
    }

    /**
     * @param bundle        数据源
     * @param isAllowRepeat 是否允许重复
     */
    public void onCreate(Bundle bundle, boolean isAllowRepeat) {
        if (bundle == null) {
            mItems = new LinkedHashSet<>();
        } else {
            // 获取缓存的数据
            List<MultiMedia> saved = bundle.getParcelableArrayList(STATE_SELECTION);
            if (saved != null) {
                if (isAllowRepeat) {
                    mItems = new LinkedHashSet<>();
                    mItems.addAll(saved);
                } else {
                    mItems = new LinkedHashSet<>(saved);
                }
            }

            mCollectionType = bundle.getInt(STATE_COLLECTION_TYPE, COLLECTION_UNDEFINED);
        }
    }

    /**
     * 缓存数据
     *
     * @param outState 缓存
     */
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(STATE_SELECTION, new ArrayList<>(mItems));
        outState.putInt(STATE_COLLECTION_TYPE, mCollectionType);
    }

    /**
     * 将数据保存进Bundle并且返回
     *
     * @return Bundle
     */
    public Bundle getDataWithBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(STATE_SELECTION, new ArrayList<>(mItems));
        bundle.putInt(STATE_COLLECTION_TYPE, mCollectionType);
        return bundle;
    }

    /**
     * 将资源对象添加到已选中集合
     *
     * @param item 数据
     */
    public boolean add(MultiMedia item) {
        if (typeConflict(item)) {
            throw new IllegalArgumentException("Can't select images and videos at the same time.");
        }
        boolean added = mItems.add(item);
        // 如果只选中了图片Item， mCollectionType设置为COLLECTION_IMAGE
        // 如果只选中了图片影音资源，mCollectionType设置为COLLECTION_IMAGE
        // 如果两种都选择了，mCollectionType设置为COLLECTION_MIXED
        if (added) {
            // 如果是空的数据源
            if (mCollectionType == COLLECTION_UNDEFINED) {
                if (item.isImage()) {
                    // 如果是图片，就设置图片类型
                    mCollectionType = COLLECTION_IMAGE;
                } else if (item.isVideo()) {
                    // 如果是视频，就设置视频类型
                    mCollectionType = COLLECTION_VIDEO;
                }
            } else if (mCollectionType == COLLECTION_IMAGE) {
                // 如果当前是图片类型
                if (item.isVideo()) {
                    // 选择了视频，就设置混合模式
                    mCollectionType = COLLECTION_MIXED;
                }
            } else if (mCollectionType == COLLECTION_VIDEO) {
                // 如果当前是图片类型
                if (item.isImage()) {
                    // 选择了图片，就设置混合模式
                    mCollectionType = COLLECTION_MIXED;
                }
            }
        }
        return added;
    }

    /**
     * 删除数据源某项
     *
     * @param item 数据
     * @return 是否删除成功
     */
    public boolean remove(MultiMedia item) {
        boolean removed;
        MultiMedia multiMedia = MultiMediaUtils.checkedMultiMediaOf(new ArrayList<>(mItems), item);
        removed = mItems.remove(multiMedia);
        if (removed) {
            if (mItems.size() == 0) {
                // 如果删除后没有数据，设置当前类型为空
                mCollectionType = COLLECTION_UNDEFINED;
            } else {
                if (mCollectionType == COLLECTION_MIXED) {
                    currentMaxSelectable();
                    Log.d("currentMaxSelectable", "currentMaxSelectable");
                }
            }
        }
        return removed;
    }

    /**
     * 重置数据源
     *
     * @param items          数据源
     * @param collectionType 类型
     */
    public void overwrite(ArrayList<MultiMedia> items, int collectionType) {
        if (items.size() == 0) {
            mCollectionType = COLLECTION_UNDEFINED;
        } else {
            mCollectionType = collectionType;
        }
        mItems.clear();
        mItems.addAll(items);
    }

    /**
     * 转换成list
     *
     * @return list<Item>
     */
    public List<MultiMedia> asList() {
        return new ArrayList<>(mItems);
    }

    /**
     * 获取uri的集合
     *
     * @return list<Uri>
     */
    public List<Uri> asListOfUri() {
        List<Uri> uris = new ArrayList<>();
        for (MultiMedia item : mItems) {
            if (item.getMediaUri() != null) {
                uris.add(item.getMediaUri());
            } else {
                uris.add(item.getUri());
            }
        }
        return uris;
    }

    /**
     * 获取path的集合
     *
     * @return list<path>
     */
    public List<String> asListOfString() {
        List<String> paths = new ArrayList<>();
        for (MultiMedia item : mItems) {
            if (item.getMediaUri() != null) {
                paths.add(PathUtils.getPath(mContext, item.getMediaUri()));
            } else {
                paths.add(PathUtils.getPath(mContext, item.getUri()));
            }

        }
        return paths;
    }

    /**
     * 该item是否在选择中
     *
     * @param item 数据源
     * @return 返回是否选择
     */
    public boolean isSelected(MultiMedia item) {
        return mItems.contains(item);
    }

    /**
     * 验证当前item是否满足可以被选中的条件
     *
     * @param item 数据item
     * @return 弹窗
     */
    public IncapableCause isAcceptable(MultiMedia item) {
        boolean maxSelectableReached = false;
        int maxSelectable = 0;
        // 判断是否混合视频图片模式
        if (!AlbumSpec.getInstance().mediaTypeExclusive) {
            // 混合检查
            getSelectCount();
            GlobalSpec spec = GlobalSpec.getInstance();
            if (item.getMimeType().startsWith(IMAGE)) {
                if (mSelectedImageCount == spec.maxImageSelectable) {
                    maxSelectableReached = true;
                    maxSelectable = spec.maxVideoSelectable;
                }
            } else if (item.getMimeType().startsWith(VIDEO)) {
                if (mSelectedVideoCount == spec.maxVideoSelectable) {
                    maxSelectableReached = true;
                    maxSelectable = spec.maxVideoSelectable;
                }
            }
        } else {
            // 非混合模式
            maxSelectableReached = maxSelectableReached();
            maxSelectable = currentMaxSelectable();
        }

        return newIncapableCause(item, maxSelectableReached, maxSelectable);
    }

    /**
     * 验证当前item是否满足可以被选中的条件
     * @param item 数据item
     * @param maxSelectableReached 是否已经选择最大值
     * @param maxSelectable 选择的最大数量
     * @return 弹窗
     */
    public IncapableCause newIncapableCause(MultiMedia item, boolean maxSelectableReached, int maxSelectable) {
        // 检查是否超过最大设置数量
        if (maxSelectableReached) {
            String cause;

            try {
                cause = mContext.getResources().getString(
                        R.string.z_multi_library_error_over_count,
                        maxSelectable
                );
            } catch (Resources.NotFoundException e) {
                cause = mContext.getString(
                        R.string.z_multi_library_error_over_count,
                        maxSelectable
                );
            } catch (NoClassDefFoundError e) {
                cause = mContext.getString(
                        R.string.z_multi_library_error_over_count,
                        maxSelectable
                );
            }
            // 生成窗口
            return new IncapableCause(cause);
        } else if (typeConflict(item)) {
            // 判断选择资源(图片跟视频)是否类型冲突
            return new IncapableCause(mContext.getString(R.string.z_multi_library_error_type_conflict));
        }

        // 过滤文件
        return PhotoMetadataUtils.isAcceptable(mContext, item);
    }

    /**
     * 当前数量 和 当前选择最大数量比较 是否相等
     *
     * @return boolean
     */
    public boolean maxSelectableReached() {
        return mItems.size() == currentMaxSelectable();
    }

    /**
     * 获取当前选择了图片、视频数量
     */
    private void getSelectCount() {
        mSelectedImageCount = 0;
        mSelectedVideoCount = 0;
        List<MultiMedia> list = new ArrayList<>(mItems);
        for (MultiMedia multiMedia : list) {
            if (multiMedia.getMimeType().startsWith("image")) {
                mSelectedImageCount++;
            } else if (multiMedia.getMimeType().startsWith("video")) {
                mSelectedVideoCount++;
            }
        }
    }

    /**
     * 返回最多选择的数量
     *
     * @return 数量
     */
    private int currentMaxSelectable() {
        GlobalSpec spec = GlobalSpec.getInstance();
        int leastCount;
        // 判断是否能同时选择视频和图片
        if (!AlbumSpec.getInstance().mediaTypeExclusive) {
            // 返回视频+图片
            leastCount = spec.maxImageSelectable + spec.maxVideoSelectable;
        } else {
            if (mCollectionType == COLLECTION_IMAGE) {
                leastCount = spec.maxImageSelectable;
            } else if (mCollectionType == COLLECTION_VIDEO) {
                leastCount = spec.maxVideoSelectable;
            } else {
                // 返回视频+图片
                leastCount = spec.maxImageSelectable + spec.maxVideoSelectable;
            }

        }

        return leastCount;
    }

    /**
     * 判断选择资源(图片跟视频)是否类型冲突
     * Determine whether there will be conflict media types. A user can only select images and videos at the same time
     * while {@link AlbumSpec#mediaTypeExclusive} is set to false.
     */
    private boolean typeConflict(MultiMedia item) {
        // 是否可以同时选择不同的资源类型 true表示不可以 false表示可以
        return AlbumSpec.getInstance().mediaTypeExclusive
                && ((item.isImage() && (mCollectionType == COLLECTION_VIDEO || mCollectionType == COLLECTION_MIXED))
                || (item.isVideo() && (mCollectionType == COLLECTION_IMAGE || mCollectionType == COLLECTION_MIXED)));
    }

    /**
     * 获取数据源长度
     *
     * @return 数据源长度
     */
    public int count() {
        return mItems.size();
    }

    /**
     * 返回选择的num
     *
     * @param item 数据
     * @return 选择的索引，最终返回的选择了第几个
     */
    public int checkedNumOf(MultiMedia item) {
        return MultiMediaUtils.checkedNumOf(new ArrayList<>(mItems), item);
    }

}
