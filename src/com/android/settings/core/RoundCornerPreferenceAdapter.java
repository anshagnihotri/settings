/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.core;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.widget.SettingsThemeHelper;
import com.android.settingslib.widget.theme.R;

import java.util.ArrayList;
import java.util.List;

public class RoundCornerPreferenceAdapter extends PreferenceGroupAdapter {

    private static final int ROUND_CORNER_CENTER = 1;
    private static final int ROUND_CORNER_TOP = 1 << 1;
    private static final int ROUND_CORNER_BOTTOM = 1 << 2;

    private final PreferenceGroup mPreferenceGroup;

    private List<Integer> mRoundCornerMappingList;

    private final Handler mHandler;

    private final Runnable mSyncRunnable = new Runnable() {
        @Override
        public void run() {
            updatePreferences();
        }
    };

    public RoundCornerPreferenceAdapter(@NonNull PreferenceGroup preferenceGroup) {
        super(preferenceGroup);
        mPreferenceGroup = preferenceGroup;
        mHandler = new Handler(Looper.getMainLooper());
        updatePreferences();
    }

    @Override
    public void onPreferenceHierarchyChange(@NonNull Preference preference) {
        super.onPreferenceHierarchyChange(preference);
        mHandler.removeCallbacks(mSyncRunnable);
        mHandler.post(mSyncRunnable);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        bindDsCategorySpacing(holder, position);
        updateBackground(holder, position);
    }

    private void bindDsCategorySpacing(@NonNull PreferenceViewHolder holder, int position) {
        final View itemView = holder.itemView;
        if (itemView.getId() == com.android.settings.R.id.ds_untitled_category_space) {
            final ViewGroup.LayoutParams lp = itemView.getLayoutParams();
            if (lp != null) {
                final int targetHeight = itemView.getResources().getDimensionPixelSize(
                        position == 0
                                ? com.android.settings.R.dimen.ds_space_2
                                : com.android.settings.R.dimen.ds_section_gap_top);
                if (lp.height != targetHeight) {
                    lp.height = targetHeight;
                    itemView.setLayoutParams(lp);
                }
            }
            return;
        }

        if (itemView.getId() == com.android.settings.R.id.ds_category_root
                || holder.findViewById(com.android.settings.R.id.ds_category_root) != null) {
            final Preference preference = getItem(position);
            final boolean hasTitle = preference != null && !TextUtils.isEmpty(preference.getTitle());
            final int horizontalPadding = itemView.getResources().getDimensionPixelSize(
                    com.android.settings.R.dimen.ds_row_screen_padding_horizontal);
            final int topPadding = itemView.getResources().getDimensionPixelSize(
                    position == 0
                            ? (hasTitle
                                    ? com.android.settings.R.dimen.ds_space_3
                                    : com.android.settings.R.dimen.ds_space_2)
                            : com.android.settings.R.dimen.ds_section_gap_top);
            itemView.setPaddingRelative(
                    horizontalPadding,
                    topPadding,
                    horizontalPadding,
                    itemView.getPaddingBottom());
        }
    }

    protected @DrawableRes int getRoundCornerDrawableRes(int position, boolean isSelected) {
        int CornerType = mRoundCornerMappingList.get(position);

        if ((CornerType & ROUND_CORNER_CENTER) == 0) {
            return 0;
        }

        if (((CornerType & ROUND_CORNER_TOP) != 0) && ((CornerType & ROUND_CORNER_BOTTOM) == 0)) {
            // the first
            return com.android.settings.R.drawable.redesign_round_background_top;
        } else if (((CornerType & ROUND_CORNER_BOTTOM) != 0)
                && ((CornerType & ROUND_CORNER_TOP) == 0)) {
            // the last
            return com.android.settings.R.drawable.redesign_round_background_bottom;
        } else if (((CornerType & ROUND_CORNER_TOP) != 0)
                && ((CornerType & ROUND_CORNER_BOTTOM) != 0)) {
            // the only one preference
            return com.android.settings.R.drawable.redesign_round_background_single;
        } else {
            // in the center
            return com.android.settings.R.drawable.redesign_round_background_center;
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    private void updatePreferences() {
        mRoundCornerMappingList = new ArrayList<>();
        mappingPreferenceGroup(mRoundCornerMappingList, mPreferenceGroup);
    }

    private void mappingPreferenceGroup(List<Integer> visibleList, PreferenceGroup group) {
        int groupSize = group.getPreferenceCount();
        int firstVisible = 0;
        int lastVisible = 0;
        for (int i = 0; i < groupSize; i++) {
            Preference pref = group.getPreference(i);
            if (!pref.isVisible()) {
                continue;
            }

            //the first visible preference.
            Preference firstVisiblePref = group.getPreference(firstVisible);
            if (!firstVisiblePref.isVisible()) {
                firstVisible = i;
            }

            int value = 0;
            if (group instanceof PreferenceCategory) {
                if (pref instanceof PreferenceCategory) {
                    visibleList.add(value);
                    mappingPreferenceGroup(visibleList, (PreferenceCategory) pref);
                } else {
                    if (i == firstVisible) {
                        value |= ROUND_CORNER_TOP;
                    }

                    value |= ROUND_CORNER_BOTTOM;
                    if (i > lastVisible) {
                        // the last
                        int lastIndex = visibleList.size() - 1;
                        int newValue = visibleList.get(lastIndex) & ~ROUND_CORNER_BOTTOM;
                        visibleList.set(lastIndex, newValue);
                        lastVisible = i;
                    }

                    value |= ROUND_CORNER_CENTER;
                    visibleList.add(value);
                }
            } else {
                visibleList.add(value);
                if (pref instanceof PreferenceCategory) {
                    mappingPreferenceGroup(visibleList, (PreferenceCategory) pref);
                }
            }
        }
    }

    /** handle roundCorner background */
    private void updateBackground(PreferenceViewHolder holder, int position) {
        @DrawableRes int backgroundRes = getRoundCornerDrawableRes(position, false /* isSelected*/);

        View v = holder.itemView;
        v.setBackgroundResource(backgroundRes);
    }
}
