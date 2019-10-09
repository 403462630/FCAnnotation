package com.fc.annotation.example;

import android.view.View;
import com.fc.annotation.Debounce;

public class ExampleTest {

    class ViewOnClickListener {
        @Debounce
        public final void onClick(View v) {

        }
    }

    @Debounce
    public final void test() {

    }
}
