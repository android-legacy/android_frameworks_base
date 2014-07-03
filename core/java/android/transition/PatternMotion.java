/*
 * Copyright (C) 2014 The Android Open Source Project
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
package android.transition;

import com.android.internal.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.PathParser;

/**
 * A PathMotion that takes a Path pattern and applies it to the separation between two points.
 * The starting point of the Path will be moved to the origin and the end point will be scaled
 * and rotated so that it matches with the target end point.
 * <p>This may be used in XML as an element inside a transition.</p>
 * <pre>
 * {@code
 * &lt;changeBounds>
 *     &lt;patternMotion android:pathData="M0 0 L0 100 L100 100"/>
 * &lt;/changeBounds>
 * }
 * </pre>
 */
public class PatternMotion extends PathMotion {

    private Path mOriginalPattern;

    private final Path mPattern = new Path();

    private final Matrix mTempMatrix = new Matrix();

    /**
     * Constructs a PatternMotion with a straight-line pattern.
     */
    public PatternMotion() {
        mPattern.lineTo(1, 0);
        mOriginalPattern = mPattern;
    }

    public PatternMotion(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PatternMotion);
        try {
            String pathData = a.getString(R.styleable.PatternMotion_pathData);
            if (pathData == null) {
                throw new RuntimeException("pathData must be supplied for patternMotion");
            }
            Path pattern = PathParser.createPathFromPathData(pathData);
            setPattern(pattern);
        } finally {
            a.recycle();
        }

    }

    /**
     * Creates a PatternMotion with the Path defining a pattern of motion between two coordinates.
     * The pattern will be translated, rotated, and scaled to fit between the start and end points.
     * The pattern must not be empty and must have the end point differ from the start point.
     *
     * @param pattern A Path to be used as a pattern for two-dimensional motion.
     */
    public PatternMotion(Path pattern) {
        setPattern(pattern);
    }

    /**
     * Returns the Path defining a pattern of motion between two coordinates.
     * The pattern will be translated, rotated, and scaled to fit between the start and end points.
     * The pattern must not be empty and must have the end point differ from the start point.
     *
     * @return the Path defining a pattern of motion between two coordinates.
     * @attr ref android.R.styleable#PatternMotion_pathData
     */
    public Path getPattern() {
        return mOriginalPattern;
    }

    /**
     * Sets the Path defining a pattern of motion between two coordinates.
     * The pattern will be translated, rotated, and scaled to fit between the start and end points.
     * The pattern must not be empty and must have the end point differ from the start point.
     *
     * @param pattern A Path to be used as a pattern for two-dimensional motion.
     * @attr ref android.R.styleable#PatternMotion_pathData
     */
    public void setPattern(Path pattern) {
        PathMeasure pathMeasure = new PathMeasure(pattern, false);
        float length = pathMeasure.getLength();
        float[] pos = new float[2];
        pathMeasure.getPosTan(length, pos, null);
        float endX = pos[0];
        float endY = pos[1];
        pathMeasure.getPosTan(0, pos, null);
        float startX = pos[0];
        float startY = pos[1];

        if (startX == endX && startY == endY) {
            throw new IllegalArgumentException("pattern must not end at the starting point");
        }

        mTempMatrix.setTranslate(-startX, -startY);
        float dx = endX - startX;
        float dy = endY - startY;
        float distance = distance(dx, dy);
        float scale = 1 / distance;
        mTempMatrix.postScale(scale, scale);
        double angle = Math.atan2(dy, dx);
        mTempMatrix.postRotate((float) Math.toDegrees(-angle));
        pattern.transform(mTempMatrix, mPattern);
        mOriginalPattern = pattern;
    }

    @Override
    public Path getPath(float startX, float startY, float endX, float endY) {
        float dx = endX - startX;
        float dy = endY - startY;
        float length = distance(dx, dy);
        double angle = Math.atan2(dy, dx);

        mTempMatrix.setScale(length, length);
        mTempMatrix.postRotate((float) Math.toDegrees(angle));
        mTempMatrix.postTranslate(startX, startY);
        Path path = new Path();
        mPattern.transform(mTempMatrix, path);
        return path;
    }

    private static float distance(float x, float y) {
        return FloatMath.sqrt((x * x) + (y * y));
    }
}