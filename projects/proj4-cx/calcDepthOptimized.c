// CS 61C Fall 2015 Project 4

// include SSE intrinsics
#if defined(_MSC_VER)
#include <intrin.h>
#elif defined(__GNUC__) && (defined(__x86_64__) || defined(__i386__))
#include <x86intrin.h>
#endif

// include OpenMP
#if !defined(_MSC_VER)
#include <pthread.h>
#endif
#include <omp.h>

#include <math.h>
#include <stdbool.h>
#include <stdio.h>

#include "calcDepthOptimized.h"
#include "calcDepthNaive.h"

/* DO NOT CHANGE ANYTHING ABOVE THIS LINE. */
float displacementOptimized(int x, int y) {
    return x*x + y*y;
}

void calcDepthOptimized(float *depth, float *left, float *right,
                        int imageWidth, int imageHeight, int featureWidth,
                        int featureHeight, int maximumDisplacement) {

    memset(depth, 0, sizeof(float) * imageWidth * imageHeight);
    int maxY = imageHeight - featureHeight;
    int maxX = imageWidth - featureWidth;

    #pragma omp parallel for
    for (int y = featureHeight; y < maxY; y++) {
        int beginY = -maximumDisplacement > -y + featureHeight ?
                     -maximumDisplacement : -y + featureHeight;
        int endY = imageHeight - featureHeight - y;
        int fullX = 2 * featureWidth;
        int fullY = 2 * featureHeight;
        int range16 = fullX - fullX%16, range4 = fullX - fullX%4;

        for (int x = featureWidth; x < maxX; x++) {
            __m128 leftv, rightv, difference, sq_diffs;
            float diff;
            float minsquaredDiff = -1;
            int minDy = 0;
            int minDx = 0;
            int beginX = -maximumDisplacement > -x + featureWidth ?
                         -maximumDisplacement : -x + featureWidth;
            int endX = imageWidth - featureWidth - x;
            int x_l = x - featureWidth, y_l = y - featureWidth;

            for (int dx = beginX; dx <= maximumDisplacement && dx < endX; dx++) {
                int x_r = x_l + dx;

                for (int dy = beginY; dy <= maximumDisplacement && dy  < endY;
                     dy++) {
                    __m128 sq_diffs_sum = _mm_setzero_ps();
                    int leftX, leftY, rightX, rightY;
                    float squaredDiff = 0;
                    int y_r = y_l + dy;

                    for (int boxX = 0; boxX < range16; boxX += 16) {
                        leftX = x_l + boxX;
                        rightX = x_r + boxX;

                        for (int boxY = 0; boxY <= fullY; boxY++) {
                            leftY = y_l + boxY;
                            rightY = y_r + boxY;
                            int leftOffset = leftY * imageWidth + leftX;
                            int rightOffset =  rightY * imageWidth + rightX;
                            for (int i = 0; i < 4; i++) {
                                leftv = _mm_loadu_ps(left + leftOffset + i * 4);
                                rightv = _mm_loadu_ps(right + rightOffset+ i * 4);
                                difference = _mm_sub_ps(leftv, rightv);
                                sq_diffs = _mm_mul_ps(difference, difference);
                                sq_diffs_sum = _mm_add_ps(sq_diffs_sum, sq_diffs);
                            }
                        }
                    }

                    for (int boxX = range16; boxX < range4; boxX += 4) {
                        leftX = x_l + boxX;
                        rightX = x_r + boxX;

                        for (int boxY = 0; boxY <= fullY; boxY++) {
                            leftY = y_l + boxY;
                            rightY = y_r + boxY;
                            leftv = _mm_loadu_ps(left + leftY * imageWidth + leftX);
                            rightv = _mm_loadu_ps(right + rightY * imageWidth +
                                                  rightX);
                            difference = _mm_sub_ps(leftv, rightv);
                            sq_diffs = _mm_mul_ps(difference, difference);
                            sq_diffs_sum = _mm_add_ps(sq_diffs_sum, sq_diffs);
                        }
                    }

                    for (int bx = range4; bx <= fullX; bx++) {
                        for (int by = 0; by <= fullY; by++) {
                            leftX = x_l + bx;
                            leftY = y_l + by;
                            rightX = x_r + bx;
                            rightY = y_r + by;
                            diff = left[leftY * imageWidth + leftX]
                                   - right[rightY * imageWidth + rightX];
                            squaredDiff += diff * diff;
                        }
                    }

                    squaredDiff += sq_diffs_sum[0] + sq_diffs_sum[1]
                                         + sq_diffs_sum[2] + sq_diffs_sum[3];

                    if ((minsquaredDiff == -1)
                        || (minsquaredDiff > squaredDiff)
                        || ((minsquaredDiff == squaredDiff)
                            && (displacementOptimized(dx, dy) <
                                displacementOptimized(minDx, minDy)))) {
                        minsquaredDiff = squaredDiff;
                        minDx = dx;
                        minDy = dy;
                    }
                }
            }

            if (minsquaredDiff != -1 && maximumDisplacement != 0) {
                depth[y * imageWidth + x] = displacementNaive(minDx, minDy);
            }
        }
    }
}
