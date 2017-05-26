/*
 * Copyright (C) 2016 stfalcon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stfalcon.frescoimageviewer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

/*
 * Created by Alexander Krol (troy379) on 29.08.16.
 */
class SwipeToDismissListener implements View.OnTouchListener {

    private static final String PROPERTY_TRANSLATION_Y = "translationY";
    private static final String PROPERTY_TRANSLATION_X = "translationX";

    private final View swipeView;
    private int translationLimit;
    private OnDismissListener dismissListener;
    private OnViewMoveListener moveListener;

    public SwipeToDismissListener(View swipeView, OnDismissListener dismissListener,
                                  OnViewMoveListener moveListener) {
        this.swipeView = swipeView;
        this.dismissListener = dismissListener;
        this.moveListener = moveListener;
    }

    private boolean tracking = false;
    private float startY;
    private float startX;
    private VelocityTracker velocityTracker = null;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        translationLimit = v.getHeight() / 4;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                velocityTracker = VelocityTracker.obtain();
                velocityTracker.addMovement(event);
                Rect hitRect = new Rect();
                swipeView.getHitRect(hitRect);
                if (hitRect.contains((int) event.getX(), (int) event.getY())) {
                    tracking = true;
                }
                startY = event.getY();
                startX = event.getX();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (tracking) {
                    tracking = false;
                    animateSwipeView(v.getHeight(), velocityTracker);
                }
                if (velocityTracker != null) velocityTracker.recycle();
                velocityTracker = null;
                return true;
            case MotionEvent.ACTION_MOVE:
                velocityTracker.addMovement(event);
                if (tracking) {
                    float translationY = event.getY() - startY;
                    float translationX = event.getX() - startX;
                    swipeView.setTranslationY(translationY);
                    swipeView.setTranslationX(translationX);
                    callMoveListener(translationY, translationLimit);
                }
                return true;
        }
        return false;
    }

    private void animateSwipeView(int parentHeight, VelocityTracker velocityTracker) {
        velocityTracker.computeCurrentVelocity(1);
        float currentY = swipeView.getTranslationY();
        float animateToY = 0.0f;
        float currentX = swipeView.getTranslationX();
        float animateToX = 0f;

        float d = (float) (Math.signum(currentY)
                * Math.sqrt(currentY * currentY + currentX * currentX));
        if (Math.abs(velocityTracker.getYVelocity()) > 1) {
            animateToY = Math.signum(currentY) * parentHeight;
        } else{
            if (d < -translationLimit) {
                animateToY = -parentHeight;
            } else if (d > translationLimit) {
                animateToY = parentHeight;
            }
        }

        float dy = animateToY - currentY;
        if (animateToY != 0) {
            float dx = dy / currentY * currentX;
            animateToX = currentX + dx;
        }

        AnimatorSet animSet = new AnimatorSet();
        final boolean isDismissed = animateToY != 0.0f;
        ObjectAnimator animator = ObjectAnimator.ofFloat(
                swipeView, PROPERTY_TRANSLATION_Y, currentY, animateToY);
        ObjectAnimator animatorX = ObjectAnimator.ofFloat(
                swipeView, PROPERTY_TRANSLATION_X, currentX, animateToX);
        animSet.playTogether(animator, animatorX);

        animSet.setDuration(200);
        animSet.setInterpolator(new AccelerateInterpolator());
        animSet.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (isDismissed) callDismissListener();
                    }
                });
        animator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        callMoveListener((float) animation.getAnimatedValue(), translationLimit);
                    }
                });
        animSet.start();
    }

    private void callDismissListener() {
        if (dismissListener != null) {
            dismissListener.onDismiss();
        }
    }

    private void callMoveListener(float translationY, int translationLimit) {
        if (moveListener != null) {
            moveListener.onViewMove(translationY, translationLimit);
        }
    }

    interface OnViewMoveListener {
        void onViewMove(float translationY, int translationLimit);
    }
}