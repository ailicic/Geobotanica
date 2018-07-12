//package com.geobotanica.geobotanica.util
//
//import android.support.constraint.ConstraintLayout
//import android.support.constraint.ConstraintSet
//import android.util.TypedValue
//import android.view.View
//
//var pixelsPerDp = 0F
//
//fun View.addToConstraintLayout(constraintLayout: ConstraintLayout,
////                               pixelsPerDp: Float,
//                               topAt: Int? = null,
//                               below: Int? = null,
//                               startAt: Int? = null,
//                               startAfter: Int? = null,
//                               endAt: Int? = null,
////                               topMarginDp: Int = (8 * pixelsPerDp).toInt(),
//                               topMarginDp: Int = 8,
////                               horizMarginDp: Int = (16  * pixelsPerDp).toInt() )
//                               horizMarginDp: Int = 16 )
//{
//    if ( pixelsPerDp == 0F)
//        pixelsPerDp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,1f, resources.displayMetrics)
//    val topMargin = (topMarginDp * pixelsPerDp).toInt()
//    val horizMargin = (horizMarginDp * pixelsPerDp).toInt()
//    constraintLayout.addView(this)
//
//    val constraintSet = ConstraintSet()
//    constraintSet.clone(constraintLayout)
//    topAt?.let { constraintSet.connect(id, ConstraintSet.TOP, topAt, ConstraintSet.TOP, topMargin) }
//    below?.let { constraintSet.connect(id, ConstraintSet.TOP, below, ConstraintSet.BOTTOM, topMargin) }
//    startAt?.let { constraintSet.connect(id, ConstraintSet.START, startAt, ConstraintSet.START, horizMargin) }
//    startAfter?.let { constraintSet.connect(id, ConstraintSet.START, startAfter, ConstraintSet.END, horizMargin) }
//    endAt?.let { constraintSet.connect(id, ConstraintSet.END, endAt, ConstraintSet.END, horizMargin) }
//    constraintSet.applyTo(constraintLayout)
//}