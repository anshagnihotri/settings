/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.TwoStatePreference;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.accessibility.AccessibilityUtil;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.widget.Expandable;
import com.android.settingslib.widget.SettingsPreferenceGroupAdapter;
import com.android.settingslib.widget.SettingsThemeHelper;

import com.google.android.material.appbar.AppBarLayout;

public class HighlightablePreferenceGroupAdapter extends SettingsPreferenceGroupAdapter {

    private static final String TAG = "HighlightableAdapter";
    @VisibleForTesting static final long DELAY_COLLAPSE_DURATION_MILLIS = 300L;
    @VisibleForTesting static final long DELAY_HIGHLIGHT_DURATION_MILLIS = 600L;
    @VisibleForTesting static final long DELAY_HIGHLIGHT_DURATION_MILLIS_A11Y = 300L;
    @VisibleForTesting static final long HIGHLIGHT_DURATION = 15000L;
    private static final int HIGHLIGHT_FADE_OUT_DURATION = 500;
    private static final int HIGHLIGHT_FADE_IN_DURATION = 200;

    @VisibleForTesting @DrawableRes final int mHighlightBackgroundRes;
    @VisibleForTesting boolean mHighlightVisible;

    private final Context mContext;
    private final PreferenceGroup mRootGroup;
    private final @DrawableRes int mNormalBackgroundRes;
    private final @Nullable String mHighlightKey;
    private boolean mHighlightRequested;
    private int mHighlightPosition = RecyclerView.NO_POSITION;

    /**
     * Tries to override initial expanded child count.
     *
     * <p>Initial expanded child count will be ignored if: 1. fragment contains request to highlight
     * a particular row. 2. count value is invalid.
     */
    public static void adjustInitialExpandedChildCount(SettingsPreferenceFragment host) {
        if (host == null) {
            return;
        }
        final PreferenceScreen screen = host.getPreferenceScreen();
        if (screen == null) {
            return;
        }
        final Bundle arguments = host.getArguments();
        if (arguments != null) {
            final String highlightKey = arguments.getString(EXTRA_FRAGMENT_ARG_KEY);
            if (!TextUtils.isEmpty(highlightKey)) {
                // Has highlight row - expand everything
                screen.setInitialExpandedChildrenCount(Integer.MAX_VALUE);
                return;
            }
        }

        final int initialCount = host.getInitialExpandedChildCount();
        if (initialCount <= 0) {
            return;
        }
        screen.setInitialExpandedChildrenCount(initialCount);
    }

    public HighlightablePreferenceGroupAdapter(
            @NonNull PreferenceGroup preferenceGroup,
            @Nullable String key,
            boolean highlightRequested) {
        super(preferenceGroup);
        mRootGroup = preferenceGroup;
        mHighlightKey = key;
        mHighlightRequested = highlightRequested;
        mContext = preferenceGroup.getContext();
        final TypedValue outValue = new TypedValue();
        mNormalBackgroundRes = R.drawable.preference_background;
        mHighlightBackgroundRes = R.drawable.preference_background_highlighted;
    }

    public HighlightablePreferenceGroupAdapter(
            @NonNull PreferenceGroup preferenceGroup,
            @Nullable String key,
            boolean highlightRequested,
            java.util.Map<String, com.android.settingslib.widget.FooterData> footerDataMap) {
        super(preferenceGroup, footerDataMap);

        mRootGroup = preferenceGroup;
        mHighlightKey = key;
        mHighlightRequested = highlightRequested;
        mContext = preferenceGroup.getContext();
        final TypedValue outValue = new TypedValue();
        mNormalBackgroundRes = R.drawable.preference_background;
        mHighlightBackgroundRes = R.drawable.preference_background_highlighted;
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        bindDsRowViews(holder, position);
        updateBackground(holder, position);
    }

    /**
     * Normalizes horizontal row/category padding after
     * {@link SettingsPreferenceGroupAdapter#onBindViewHolder} (which overrides start/end padding in
     * expressive mode), adjusts category top spacing (24dp between groups, compact at position 0),
     * binds opt-in right-aligned value extras on standard {@link Preference} instances, and shows
     * {@code R.id.ds_chevron} strictly on drill-down/dialog rows that do not have a trailing
     * widget.
     */
    private void bindDsRowViews(@NonNull PreferenceViewHolder holder, int position) {
        final View itemView = holder.itemView;
        final Preference preference = getItem(position);

        if (itemView.getId() == R.id.ds_untitled_category_space) {
            final ViewGroup.LayoutParams lp = itemView.getLayoutParams();
            if (lp != null) {
                final int targetHeight = itemView.getResources().getDimensionPixelSize(
                        position == 0 ? R.dimen.ds_space_2 : R.dimen.ds_section_gap_top);
                if (lp.height != targetHeight) {
                    lp.height = targetHeight;
                    itemView.setLayoutParams(lp);
                }
            }
            return;
        }

        if (itemView.getId() == R.id.ds_category_root
                || holder.findViewById(R.id.ds_category_root) != null) {
            final int horizontalPadding = itemView.getResources()
                    .getDimensionPixelSize(R.dimen.ds_row_screen_padding_horizontal);
            final boolean hasTitle = preference != null && !TextUtils.isEmpty(preference.getTitle());
            final int topPadding = itemView.getResources().getDimensionPixelSize(
                    position == 0
                            ? (hasTitle ? R.dimen.ds_space_3 : R.dimen.ds_space_2)
                            : R.dimen.ds_section_gap_top);
            itemView.setPaddingRelative(
                    horizontalPadding,
                    topPadding,
                    horizontalPadding,
                    itemView.getPaddingBottom());
            return;
        }

        final View chevronView = holder.findViewById(R.id.ds_chevron);
        if (chevronView == null) {
            return;
        }

        final int horizontalPadding = itemView.getResources()
                .getDimensionPixelSize(R.dimen.ds_row_screen_padding_horizontal);
        itemView.setPaddingRelative(
                horizontalPadding,
                itemView.getPaddingTop(),
                horizontalPadding,
                itemView.getPaddingBottom());

        final View valueView = holder.findViewById(R.id.ds_row_value);
        if (valueView instanceof TextView && !(preference instanceof DsValuePreference)) {
            final TextView rightValueText = (TextView) valueView;
            CharSequence rightValue = null;
            boolean summaryMovedToRight = false;
            if (preference != null && preference.peekExtras() != null) {
                final Bundle extras = preference.peekExtras();
                rightValue = extras.getCharSequence(DsValuePreference.EXTRA_RIGHT_VALUE);
                if (TextUtils.isEmpty(rightValue)
                        && extras.getBoolean(DsValuePreference.EXTRA_SUMMARY_AS_RIGHT_VALUE, false)) {
                    rightValue = preference.getSummary();
                    summaryMovedToRight = !TextUtils.isEmpty(rightValue);
                }
            }
            if (!TextUtils.isEmpty(rightValue)) {
                rightValueText.setText(rightValue);
                rightValueText.setVisibility(View.VISIBLE);
                if (summaryMovedToRight) {
                    final View summaryView = holder.findViewById(android.R.id.summary);
                    if (summaryView != null) {
                        summaryView.setVisibility(View.GONE);
                    }
                }
            } else {
                rightValueText.setText(null);
                rightValueText.setVisibility(View.GONE);
            }
        }

        final View widgetFrame = holder.findViewById(android.R.id.widget_frame);
        final boolean hasVisibleWidget = hasVisibleWidgetChild(widgetFrame);
        final boolean isTogglePreference = (preference instanceof TwoStatePreference)
                || (preference instanceof PrimarySwitchPreference);
        final boolean hasNavigationTarget = preference != null
                && (preference.getFragment() != null
                        || preference.getIntent() != null
                        || preference.getOnPreferenceClickListener() != null
                        || preference instanceof DialogPreference);

        final boolean showChevron = preference != null
                && preference.isSelectable()
                && preference.isEnabled()
                && !isTogglePreference
                && !hasVisibleWidget
                && hasNavigationTarget;
        chevronView.setVisibility(showChevron ? View.VISIBLE : View.GONE);
    }

    private static boolean hasVisibleWidgetChild(@Nullable View widgetFrame) {
        if (widgetFrame == null || widgetFrame.getVisibility() != View.VISIBLE) {
            return false;
        }
        if (widgetFrame instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) widgetFrame;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                if (group.getChildAt(i).getVisibility() == View.VISIBLE) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    @VisibleForTesting
    void updateBackground(PreferenceViewHolder holder, int position) {
        View v = holder.itemView;
        Preference preference = getItem(position);
        if (preference != null
                && position == mHighlightPosition
                && (mHighlightKey != null && TextUtils.equals(mHighlightKey, preference.getKey()))
                && v.isShown()) {
            // set initial accessibility focus
            TextView title = (TextView) holder.findViewById(android.R.id.title);
            if (title != null) {
                ViewCompat.setAccessibilityDelegate(title, new AccessibilityDelegateCompat() {
                        @Override
                        public void onInitializeAccessibilityNodeInfo(
                                View host, AccessibilityNodeInfoCompat info) {
                            super.onInitializeAccessibilityNodeInfo(host, info);
                            info.setRequestInitialAccessibilityFocus(true);
                        }
                    });
            }
            addHighlightBackground(holder, !mHighlightVisible, position);
        } else if (Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
            // View with highlight is reused for a view that should not have highlight
            removeHighlightBackground(holder, false /* animate */, position);
        }
    }

    /**
     * A function can highlight a specific setting in recycler view. note: Before highlighting a
     * setting, screen collapses tool bar with an animation.
     */
    public void requestHighlight(View root, RecyclerView recyclerView, AppBarLayout appBarLayout) {
        if (mHighlightRequested || recyclerView == null || TextUtils.isEmpty(mHighlightKey)) {
            return;
        }

        expandGroupIfNecessary(mRootGroup, mHighlightKey);

        final int position = getPreferenceAdapterPosition(mHighlightKey);
        if (position < 0) {
            return;
        }

        // Highlight request accepted
        mHighlightRequested = true;
        // Collapse app bar after 300 milliseconds.
        if (appBarLayout != null) {
            root.postDelayed(
                    () -> appBarLayout.setExpanded(false, true),
                    DELAY_COLLAPSE_DURATION_MILLIS);
        }

        // Remove the animator as early as possible to avoid a RecyclerView crash.
        recyclerView.setItemAnimator(null);
        // Scroll to correct position after a short delay.
        root.postDelayed(
                () -> {
                    if (ensureHighlightPosition()) {
                        recyclerView.smoothScrollToPosition(mHighlightPosition);
                        highlightAndFocusTargetItem(recyclerView, mHighlightPosition);
                    }
                },
                AccessibilityUtil.isTouchExploreEnabled(mContext)
                        ? DELAY_HIGHLIGHT_DURATION_MILLIS_A11Y
                        : DELAY_HIGHLIGHT_DURATION_MILLIS);
    }

    private void highlightAndFocusTargetItem(RecyclerView recyclerView, int highlightPosition) {
        ViewHolder target = recyclerView.findViewHolderForAdapterPosition(highlightPosition);
        if (target != null) { // view already visible
            notifyItemChanged(mHighlightPosition);
            target.itemView.requestFocus();
        } else { // otherwise we're about to scroll to that view (but we might not be scrolling yet)
            recyclerView.addOnScrollListener(
                    new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrollStateChanged(
                                @NonNull RecyclerView recyclerView, int newState) {
                            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                                notifyItemChanged(mHighlightPosition);
                                ViewHolder target =
                                        recyclerView.findViewHolderForAdapterPosition(
                                                highlightPosition);
                                if (target != null) {
                                    target.itemView.requestFocus();
                                }
                                recyclerView.removeOnScrollListener(this);
                            }
                        }
                    });
        }
    }

    /**
     * Make sure we highlight the real-wanted position in case of preference position already
     * changed when the delay time comes.
     */
    private boolean ensureHighlightPosition() {
        if (TextUtils.isEmpty(mHighlightKey)) {
            return false;
        }
        final int position = getPreferenceAdapterPosition(mHighlightKey);
        final boolean allowHighlight = position >= 0;
        if (allowHighlight && mHighlightPosition != position) {
            Log.w(TAG, "EnsureHighlight: position has changed since last highlight request");
            // Make sure RecyclerView always uses latest correct position to avoid exceptions.
            mHighlightPosition = position;
        }
        return allowHighlight;
    }

    public boolean isHighlightRequested() {
        return mHighlightRequested;
    }

    @VisibleForTesting
    void requestRemoveHighlightDelayed(PreferenceViewHolder holder, int position) {
        final View v = holder.itemView;
        v.postDelayed(
                () -> {
                    mHighlightPosition = RecyclerView.NO_POSITION;
                    mHighlightVisible = false;
                    removeHighlightBackground(holder, true /* animate */, position);
                },
                HIGHLIGHT_DURATION);
    }

    /**
     * Recursively traverses the preference tree to find the target highlight key.
     * If the key is found inside an expandable preference group, it programmatically
     * expands the group to ensure the target preference is visible before highlighting.
     *
     * @param group The current preference group being inspected.
     * @param targetKey The preference key requested to be highlighted.
     * @return True if the target key was found within this group or its children, false otherwise.
     */
    private boolean expandGroupIfNecessary(PreferenceGroup group, String targetKey) {
        if (group == null || TextUtils.isEmpty(targetKey)) {
            return false;
        }

        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference pref = group.getPreference(i);

            // Target preference found directly
            if (TextUtils.equals(pref.getKey(), targetKey)) {
                return true;
            }

            // If it's a nested group, search recursively
            if (pref instanceof PreferenceGroup) {
                if (expandGroupIfNecessary((PreferenceGroup) pref, targetKey)) {

                    // Target is inside this group. Expand it if it supports programmed expansion.
                    if (pref instanceof Expandable) {
                        ((Expandable) pref).setExpanded(true);
                        Log.d(TAG, "Automatically expanded group for target key: " + targetKey);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void addHighlightBackground(
            PreferenceViewHolder holder, boolean animate, int position) {
        final View v = holder.itemView;
        final Context context = v.getContext();
        if (context == null) {
            return;
        }

        final int backgroundFrom = getBackgroundRes(position, false);
        final int backgroundTo = getBackgroundRes(position, true);

        Object oldAnimatorTag = v.getTag(R.id.active_background_animator);
        if (oldAnimatorTag instanceof ValueAnimator) {
            ((ValueAnimator) oldAnimatorTag).cancel();
        }

        v.setTag(R.id.active_background_animator, null);
        v.setTag(R.id.preference_highlighted, true);

        Drawable backgroundFromDrawable = ContextCompat.getDrawable(context, backgroundFrom);
        Drawable backgroundToDrawable = ContextCompat.getDrawable(context, backgroundTo);

        if (!animate || backgroundFromDrawable == null || backgroundToDrawable == null) {
            // Fallback
            v.setBackgroundResource(backgroundTo);
            Log.d(TAG, "AddHighlight: Not animation requested - setting highlight background");
            holder.setIsRecyclable(false);
            requestRemoveHighlightDelayed(holder, position);
            return;
        }
        mHighlightVisible = true;

        TransitionDrawable transitionDrawable = new TransitionDrawable(
                new Drawable[]{backgroundFromDrawable, backgroundToDrawable});
        transitionDrawable.setPaddingMode(LayerDrawable.PADDING_MODE_STACK);
        v.setBackground(transitionDrawable);

        final ValueAnimator fadeInLoop =
                ValueAnimator.ofInt(0, 1);
        fadeInLoop.setDuration(HIGHLIGHT_FADE_IN_DURATION);
        fadeInLoop.setRepeatMode(ValueAnimator.REVERSE);
        fadeInLoop.setRepeatCount(4);
        v.setTag(R.id.active_background_animator, fadeInLoop);

        holder.setIsRecyclable(false);

        fadeInLoop.addListener(new AnimatorListenerAdapter() {
            private boolean mIsReversedForNext = false;

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                transitionDrawable.startTransition(HIGHLIGHT_FADE_IN_DURATION);
                mIsReversedForNext = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                super.onAnimationRepeat(animation);
                if (mIsReversedForNext) {
                    transitionDrawable.reverseTransition(HIGHLIGHT_FADE_IN_DURATION);
                } else {
                    transitionDrawable.startTransition(HIGHLIGHT_FADE_IN_DURATION);
                }
                mIsReversedForNext = !mIsReversedForNext;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                if (v.getTag(R.id.active_background_animator) == fadeInLoop) {
                    v.setTag(R.id.active_background_animator, null);
                }

                v.setBackgroundResource(backgroundTo);

                if (Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
                    requestRemoveHighlightDelayed(holder, position);
                } else {
                    mHighlightVisible = false;
                    holder.setIsRecyclable(true);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);

                if (v.getTag(R.id.active_background_animator) == fadeInLoop) {
                    v.setTag(R.id.active_background_animator, null);
                }

                if (Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
                    v.setBackgroundResource(backgroundTo);
                    requestRemoveHighlightDelayed(holder, position);
                } else {
                    v.setBackgroundResource(backgroundFrom);
                    mHighlightVisible = false;
                    holder.setIsRecyclable(true);
                }
            }
        });

        fadeInLoop.start();
        Log.d(TAG, "AddHighlight: starting fade in animation");
    }

    private void removeHighlightBackground(
            PreferenceViewHolder holder, boolean animate, int position) {
        final View v = holder.itemView;
        final Context context = v.getContext();

        int backgroundFrom = getBackgroundRes(position, true);
        int backgroundTo = getBackgroundRes(position, false);

        Object oldAnimatorTag = v.getTag(R.id.active_background_animator);
        if (oldAnimatorTag instanceof ValueAnimator) {
            ((ValueAnimator) oldAnimatorTag).cancel();
        }
        Drawable backgroundFromDrawable = ContextCompat.getDrawable(context, backgroundFrom);
        Drawable backgroundToDrawable = ContextCompat.getDrawable(context, backgroundTo);

        if (!animate || backgroundFromDrawable == null || backgroundToDrawable == null) {
            v.setBackgroundResource(backgroundTo);
            v.setTag(R.id.preference_highlighted, false);
            holder.setIsRecyclable(true);
            Log.d(TAG, "RemoveHighlight: No animation requested - setting normal background");
            return;
        }

        v.setTag(R.id.active_background_animator, null);

        if (!Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
            // Not highlighted, no-op
            Log.d(TAG, "RemoveHighlight: Not highlighted - skipping");
            holder.setIsRecyclable(true);
            return;
        }

        v.setTag(R.id.preference_highlighted, false);

        TransitionDrawable transitionDrawable = new TransitionDrawable(
                new Drawable[]{backgroundFromDrawable, backgroundToDrawable});
        v.setBackground(transitionDrawable);

        final ValueAnimator colorAnimation =
                ValueAnimator.ofInt(0, 1);
        colorAnimation.setDuration(HIGHLIGHT_FADE_OUT_DURATION);
        v.setTag(R.id.active_background_animator, colorAnimation);
        holder.setIsRecyclable(false);

        colorAnimation.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        transitionDrawable.startTransition(HIGHLIGHT_FADE_OUT_DURATION);
                    }

                    @Override
                    public void onAnimationEnd(@NonNull Animator animation) {
                        super.onAnimationEnd(animation);
                        v.setBackgroundResource(backgroundTo);

                        v.setTag(R.id.preference_highlighted, false);
                        holder.setIsRecyclable(true);

                        if (v.getTag(R.id.active_background_animator) == colorAnimation) {
                            v.setTag(R.id.active_background_animator, null);
                        }
                    }

                    @Override
                    public void onAnimationCancel(@NonNull Animator animation) {
                        super.onAnimationCancel(animation);
                        v.setBackgroundResource(backgroundTo);
                        v.setTag(R.id.preference_highlighted, false);
                        holder.setIsRecyclable(true);

                        if (v.getTag(R.id.active_background_animator) == colorAnimation) {
                            v.setTag(R.id.active_background_animator, null);
                        }
                    }
                });
        colorAnimation.start();
        Log.d(TAG, "Starting fade out animation");
    }

    private @DrawableRes int getBackgroundRes(int position, boolean isHighlighted) {
        int backgroundRes = (isHighlighted) ? mHighlightBackgroundRes : mNormalBackgroundRes;
        if (SettingsThemeHelper.isExpressiveTheme(mContext)) {
            Log.d(TAG, "[Expressive Theme] get rounded background, highlight = " + isHighlighted);
            int roundCornerResId = getRoundCornerDrawableRes(position, false, isHighlighted);
            if (roundCornerResId != 0) {
                return roundCornerResId;
            }
        }
        return backgroundRes;
    }
}
