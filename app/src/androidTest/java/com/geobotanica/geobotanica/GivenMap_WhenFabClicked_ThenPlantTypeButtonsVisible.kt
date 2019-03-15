package com.geobotanica.geobotanica


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.AndroidJUnit4
import com.geobotanica.geobotanica.ui.MainActivity
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class GivenMap_WhenFabClicked_ThenPlantTypeButtonsVisible {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION",
                    "android.permission.WRITE_EXTERNAL_STORAGE")

    @Test
    fun givenMap_WhenFabClicked_ThenPlantTypeButtonsVisible() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(700)

        val floatingActionButton = onView(
                allOf(withId(R.id.fab),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout),
                                        childAtPosition(
                                                withId(R.id.fragment),
                                                0)),
                                2),
                        isDisplayed()))
        floatingActionButton.perform(click())

        val linearLayout = onView(
                allOf(withId(R.id.buttonTree),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(R.id.fragment),
                                                0)),
                                0),
                        isDisplayed()))
        linearLayout.check(matches(isDisplayed()))

        val linearLayout2 = onView(
                allOf(withId(R.id.buttonShrub),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(R.id.fragment),
                                                0)),
                                1),
                        isDisplayed()))
        linearLayout2.check(matches(isDisplayed()))

        val linearLayout3 = onView(
                allOf(withId(R.id.buttonHerb),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(R.id.fragment),
                                                0)),
                                2),
                        isDisplayed()))
        linearLayout3.check(matches(isDisplayed()))

        val linearLayout4 = onView(
                allOf(withId(R.id.buttonGrass),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(R.id.fragment),
                                                0)),
                                3),
                        isDisplayed()))
        linearLayout4.check(matches(isDisplayed()))

        val linearLayout5 = onView(
                allOf(withId(R.id.buttonVine),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(R.id.fragment),
                                                0)),
                                4),
                        isDisplayed()))
        linearLayout5.check(matches(isDisplayed()))

        val linearLayout6 = onView(
                allOf(withId(R.id.buttonVine),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(R.id.fragment),
                                                0)),
                                4),
                        isDisplayed()))
        linearLayout6.check(matches(isDisplayed()))
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
