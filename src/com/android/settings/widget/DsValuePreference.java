/*
 * Copyright (C) 2026 Custom ROM Design System
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

package com.android.settings.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/**
 * Opt-in Preference subclass that displays a short current value on the right side of the row
 * (e.g. "English >") inside {@code @id/ds_row_value}.
 *
 * <p>If no explicit {@link #setValueText(CharSequence)} is set, this class renders
 * {@link #getSummary()} in {@code @id/ds_row_value} and hides the bottom {@code @android:id/summary}
 * view so the row stays single-line (52dp height).
 *
 * <p>Standard {@link Preference} instances can also opt in without subclassing by setting
 * {@code preference.getExtras().putCharSequence(DsValuePreference.EXTRA_RIGHT_VALUE, "...")} or
 * {@code preference.getExtras().putBoolean(DsValuePreference.EXTRA_SUMMARY_AS_RIGHT_VALUE, true)}.
 */
public class DsValuePreference extends Preference {

    public static final String EXTRA_RIGHT_VALUE = "ds_right_value";
    public static final String EXTRA_SUMMARY_AS_RIGHT_VALUE = "ds_summary_as_right_value";

    @Nullable
    private CharSequence mValueText;

    public DsValuePreference(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.ds_preference_row);
    }

    public DsValuePreference(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.ds_preference_row);
    }

    public DsValuePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.ds_preference_row);
    }

    public DsValuePreference(@NonNull Context context) {
        super(context);
        setLayoutResource(R.layout.ds_preference_row);
    }

    /** Sets an explicit right-hand value string (e.g. "English", "5 min"). */
    public void setValueText(@Nullable CharSequence valueText) {
        if (!TextUtils.equals(mValueText, valueText)) {
            mValueText = valueText;
            notifyChanged();
        }
    }

    @Nullable
    public CharSequence getValueText() {
        return mValueText;
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final TextView rightValueView = (TextView) holder.findViewById(R.id.ds_row_value);
        final View summaryView = holder.findViewById(android.R.id.summary);
        if (rightValueView == null) {
            return;
        }

        final CharSequence valueToShow =
                !TextUtils.isEmpty(mValueText) ? mValueText : getSummary();
        if (!TextUtils.isEmpty(valueToShow)) {
            rightValueView.setText(valueToShow);
            rightValueView.setVisibility(View.VISIBLE);
            if (summaryView != null && TextUtils.isEmpty(mValueText)) {
                summaryView.setVisibility(View.GONE);
            }
        } else {
            rightValueView.setVisibility(View.GONE);
        }
    }
}
