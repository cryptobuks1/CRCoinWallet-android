package com.wavesplatform.wallet.v2.ui.home.history

import android.os.Bundle
import android.view.View
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v2.ui.base.view.BaseActivity
import kotlinx.android.synthetic.main.activity_leasing_history.*


class HistoryActivity : BaseActivity() {

    override fun configLayoutRes() = R.layout.activity_leasing_history


    override fun onViewReady(savedInstanceState: Bundle?) {
        setupToolbar(toolbar_view, View.OnClickListener { onBackPressed() }, true, getString(R.string.leasing_history_toolbar_title), R.drawable.ic_toolbar_back_black)

        val fragment = HistoryFragment.newInstance().apply {
            arguments = intent.extras
        }

        openFragment(R.id.frame_fragment_container, fragment)
    }

}