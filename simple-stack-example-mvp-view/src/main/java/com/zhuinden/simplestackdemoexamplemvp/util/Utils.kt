package com.zhuinden.simplestackdemoexamplemvp.util


import android.app.Activity
import android.content.Context
import android.support.annotation.LayoutRes
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.functions.BiFunction

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.GONE
}

inline fun <T: View> T.showIf(predicate: (T) -> Boolean) {
    if(predicate(this)) {
        show()
    } else {
        hide()
    }
}

fun <A, B> combineTwo(aSource: Observable<A>, bSource: Observable<B>): Observable<Pair<A, B>> =
    Observable.combineLatest(aSource, bSource, BiFunction { t1, t2 -> t1 to t2 })

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToParent: Boolean = false) =
    LayoutInflater.from(context).inflate(layoutRes, this, attachToParent)

fun Context.hideKeyboard(view: View) {
    (getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).also { imm ->
        imm.hideSoftInputFromWindow(view.windowToken, 0);
    }
}

fun View.hideKeyboard() {
    context.hideKeyboard(this)
}

inline fun TextView.onTextChanged(crossinline textChanged: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            textChanged(s.toString())
        }
    })
}

inline fun View.onClick(crossinline click: (View) -> Unit) {
    setOnClickListener { view ->
        click(view)
    }
}