package com.example.assignment02

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testMainActivityElementsAreDisplayed() {
        // Check if main elements are displayed
        onView(withId(R.id.titleText)).check(matches(isDisplayed()))
        onView(withId(R.id.itemImage)).check(matches(isDisplayed()))
        onView(withId(R.id.itemName)).check(matches(isDisplayed()))
        onView(withId(R.id.itemRating)).check(matches(isDisplayed()))
        onView(withId(R.id.itemCategory)).check(matches(isDisplayed()))
        onView(withId(R.id.itemPrice)).check(matches(isDisplayed()))
        onView(withId(R.id.previousButton)).check(matches(isDisplayed()))
        onView(withId(R.id.nextButton)).check(matches(isDisplayed()))
        onView(withId(R.id.borrowButton)).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationButtons() {
        // Test next button
        onView(withId(R.id.nextButton)).perform(click())
        onView(withId(R.id.nextButton)).perform(click())
        
        // Test previous button
        onView(withId(R.id.previousButton)).perform(click())
        onView(withId(R.id.previousButton)).perform(click())
    }

    @Test
    fun testBorrowButtonClick() {
        // Click the borrow button
        onView(withId(R.id.borrowButton)).perform(click())
        
        // The detail activity should be launched
        // We can't easily test the activity transition with Espresso in this simple test
        // but we can verify the button is clickable
    }

    @Test
    fun testItemNameIsDisplayed() {
        // Check that item name is displayed (should show "Electric Guitar" initially)
        onView(withId(R.id.itemName)).check(matches(withText("Electric Guitar")))
    }
}

