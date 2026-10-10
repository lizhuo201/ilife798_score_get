package com.isuncy.ilife798

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.isuncy.ilife798.ui.nav.AppNav
import com.isuncy.ilife798.ui.theme.Ilife798Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Ilife798Theme {
                AppNav()
            }
        }
    }
}
